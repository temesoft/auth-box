package com.authbox.server.controller;

import com.authbox.base.dao.OauthClientDao;
import com.authbox.base.dao.OauthTokenDao;
import com.authbox.base.dao.OauthUserDao;
import com.authbox.base.exception.BadRequestException;
import com.authbox.base.model.AccessLog;
import com.authbox.base.model.AuthorizationResponseType;
import com.authbox.base.model.OauthClient;
import com.authbox.base.model.OauthScope;
import com.authbox.base.model.OauthToken;
import com.authbox.base.model.OauthUser;
import com.authbox.base.model.Organization;
import com.authbox.base.util.NetUtils;
import com.authbox.server.service.ScopeService;
import com.google.common.collect.ImmutableMap;
import io.micrometer.core.annotation.Timed;
import org.jboss.aerogear.security.otp.Totp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.authbox.base.config.Constants.MSG_INVALID_REQUEST;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_2FA_CODE;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_CLIENT_ID;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_CODE;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_PASSWORD;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_REDIRECT_URI;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_RESPONSE_TYPE;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_SCOPE;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_STATE;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_USERNAME;
import static com.authbox.base.config.Constants.OAUTH_PREFIX;
import static com.authbox.base.config.Constants.SPACE_SPLITTER;
import static com.authbox.base.model.TokenType.AUTHORIZATION_CODE;
import static com.authbox.base.util.HashUtils.sha256;
import static com.authbox.base.util.NetUtils.getIp;
import static com.authbox.server.util.RequestUtils.getRequestId;
import static com.authbox.server.util.RequestUtils.getTimeSinceRequest;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.springframework.util.ObjectUtils.isEmpty;

@Controller
@RequestMapping(OAUTH_PREFIX)
public class Oauth2AuthorizeController extends BaseController {

    private static final Logger LOGGER = LoggerFactory.getLogger(Oauth2AuthorizeController.class);
    private static final String TWO_FACTOR_AUTH_SUCCESS_ATTRIBUTE = "TWO_FACTOR_AUTH_SUCCESS_ATTRIBUTE";

    @Autowired
    protected OauthClientDao oauthClientDao;

    @Autowired
    protected OauthUserDao oauthUserDao;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected Clock defaultClock;

    @Autowired
    protected OauthTokenDao oauthTokenDao;

    @Autowired
    protected ScopeService scopeService;

    @GetMapping("/authorize")
    @Timed("getAuthorize")
    public ModelAndView getAuthorize(final HttpServletRequest req,
                                     @RequestParam(OAUTH2_ATTR_RESPONSE_TYPE) final AuthorizationResponseType responseType,
                                     @RequestParam(OAUTH2_ATTR_CLIENT_ID) final String clientId,
                                     @RequestParam(OAUTH2_ATTR_REDIRECT_URI) final String redirectUri,
                                     @RequestParam(value = OAUTH2_ATTR_STATE, required = false) final String state,
                                     @RequestParam(value = OAUTH2_ATTR_SCOPE, required = false) final String scopeStr) {
        accessLogService.create(
                AccessLog.builder()
                        .withRequestId(getRequestId())
                        .withDuration(getTimeSinceRequest()),
                "Starting Oauth2 authorization process"
        );
        final Organization organization = getOrganization(req);

        final Optional<OauthClient> oauthClient = oauthClientDao.getById(clientId);
        if (oauthClient.isEmpty()) {
            LOGGER.debug("OauthClient not found by id='{}'", clientId);
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withOrganizationId(organization.getId())
                            .withDuration(getTimeSinceRequest())
                            .withError(MSG_INVALID_REQUEST),
                    "Oauth2 client not found by id='%s'", clientId
            );
            throw new BadRequestException(MSG_INVALID_REQUEST);
        }
        if (!oauthClient.get().getOrganizationId().equals(organization.getId())) {
            LOGGER.debug("OauthClient organization_id='{}' does not match domain prefix specified organization_id='{}'", oauthClient.get().getOrganizationId(), organization.getId());
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withOrganizationId(organization.getId())
                            .withClientId(oauthClient.get().getId())
                            .withDuration(getTimeSinceRequest())
                            .withError(MSG_INVALID_REQUEST),
                    "Oauth2 client organization_id='%s' does not match domain prefix specified organization id='%s'", oauthClient.get().getOrganizationId(), organization.getId()
            );
            throw new BadRequestException(MSG_INVALID_REQUEST);
        }
        if (!oauthClient.get().isEnabled()) {
            LOGGER.debug("OauthClient is disabled id='{}'", oauthClient.get().getId());
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withOrganizationId(organization.getId())
                            .withClientId(oauthClient.get().getId())
                            .withDuration(getTimeSinceRequest())
                            .withError(MSG_INVALID_REQUEST),
                    "Oauth2 client is disabled id='%s'", oauthClient.get().getId()
            );
            throw new BadRequestException(MSG_INVALID_REQUEST);
        }

        final String scope = scopeService.getScopeStringBasedOnRequestedAndAllowed(scopeStr, oauthClient.get());

        final String stateString = isEmpty(state) ? UUID.randomUUID().toString() : state;

        if (oauthClient.get().getRedirectUrls().stream().filter(redirectUri::startsWith).findAny().isEmpty()) {
            LOGGER.debug("OauthClient approved redirect urls={} does not match requested redirect_url='{}'", oauthClient.get().getRedirectUrls(), redirectUri);
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withOrganizationId(organization.getId())
                            .withClientId(oauthClient.get().getId())
                            .withDuration(getTimeSinceRequest())
                            .withError(MSG_INVALID_REQUEST),
                    "Oauth2 client approved redirect urls=%s does not match requested redirect url='%s'", oauthClient.get().getRedirectUrls().toString(), redirectUri
            );
            return new ModelAndView(
                    "authorize",
                    createResponseBuilder(organization, responseType, clientId, redirectUri, scope, stateString, null)
                            .put("errorMessage", "Client approved redirect urls do not match requested redirect_url")
                            .build()
            );
        }

        accessLogService.create(
                AccessLog.builder()
                        .withRequestId(getRequestId())
                        .withOrganizationId(organization.getId())
                        .withClientId(oauthClient.get().getId())
                        .withDuration(getTimeSinceRequest()),
                "Displaying authorize HTML page"
        );

        return new ModelAndView(
                "authorize",
                createResponseBuilder(organization, responseType, clientId, redirectUri, scope, stateString, null).build()
        );
    }

    @PostMapping("/authorize")
    @Timed("authorizeUserCredentials")
    public ModelAndView authorizeUserCredentials(final HttpServletRequest req,
                                                 final HttpServletResponse res,
                                                 @RequestParam(OAUTH2_ATTR_RESPONSE_TYPE) final AuthorizationResponseType responseType,
                                                 @RequestParam(OAUTH2_ATTR_CLIENT_ID) final String clientId,
                                                 @RequestParam(OAUTH2_ATTR_REDIRECT_URI) final String redirectUri,
                                                 @RequestParam(OAUTH2_ATTR_STATE) final String state,
                                                 @RequestParam(OAUTH2_ATTR_SCOPE) final String scopeStr,
                                                 @RequestParam(OAUTH2_ATTR_USERNAME) final String username,
                                                 @RequestParam(OAUTH2_ATTR_PASSWORD) final String password) {
        accessLogService.create(
                AccessLog.builder()
                        .withRequestId(getRequestId())
                        .withDuration(getTimeSinceRequest()),
                "Starting Oauth2 authorization process (validate credentials)"
        );
        final Organization organization = getOrganization(req);

        final Optional<OauthClient> oauthClient = oauthClientDao.getById(clientId);
        if (oauthClient.isEmpty()) {
            LOGGER.debug("OauthClient not found by id='{}'", clientId);
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withOrganizationId(organization.getId())
                            .withDuration(getTimeSinceRequest())
                            .withError(MSG_INVALID_REQUEST),
                    "Oauth2 client not found by id='%s'", clientId
            );
            throw new BadRequestException(MSG_INVALID_REQUEST);
        }
        if (isEmpty(state)) {
            LOGGER.debug("Authorization state parameter is not provided");
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.getId())
                            .withClientId(oauthClient.get().getId())
                            .withError(MSG_INVALID_REQUEST),
                    "Authorization state parameter is not provided"
            );
            throw new BadRequestException(MSG_INVALID_REQUEST);
        }
        if (!oauthClient.get().getOrganizationId().equals(organization.getId())) {
            LOGGER.debug("OauthClient organization_id='{}' does not match domain prefix specified organization_id='{}'", oauthClient.get().getOrganizationId(), organization.getId());
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.getId())
                            .withClientId(oauthClient.get().getId())
                            .withError(MSG_INVALID_REQUEST),
                    "Oauth2 client organization id='%s' does not match domain prefix specified organization id='%s'", oauthClient.get().getOrganizationId(), organization.getId()
            );
            throw new BadRequestException(MSG_INVALID_REQUEST);
        }
        if (!oauthClient.get().isEnabled()) {
            LOGGER.debug("OauthClient is disabled id='{}'", oauthClient.get().getId());
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.getId())
                            .withClientId(oauthClient.get().getId())
                            .withError(MSG_INVALID_REQUEST),
                    "Oauth2 client is disabled id='%s'", oauthClient.get().getId()
            );
            throw new BadRequestException(MSG_INVALID_REQUEST);
        }

        final String scope = scopeService.getScopeStringBasedOnRequestedAndAllowed(scopeStr, oauthClient.get());

        if (oauthClient.get().getRedirectUrls().stream().filter(redirectUri::startsWith).findAny().isEmpty()) {
            LOGGER.debug("OauthClient approved redirect urls={} does not match requested redirect_url='{}'", oauthClient.get().getRedirectUrls(), redirectUri);
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.getId())
                            .withClientId(oauthClient.get().getId())
                            .withError(MSG_INVALID_REQUEST),
                    "Oauth2 client approved redirect urls=%s does not match requested redirect url='%s'", oauthClient.get().getRedirectUrls().toString(), redirectUri
            );
            return new ModelAndView(
                    "authorize",
                    createResponseBuilder(organization, responseType, clientId, redirectUri, scope, state, null)
                            .put("errorMessage", "Client approved redirect urls do not match requested redirect_url")
                            .build()
            );
        }

        // Authenticate user credentials
        final Optional<OauthUser> oauthUser = oauthUserDao.getByUsernameAndOrganizationId(username, organization.getId());
        if (oauthUser.isEmpty()) {
            LOGGER.debug("OauthUser not found by username='{}' and organization_id='{}'", username, organization.getId());
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.getId())
                            .withClientId(oauthClient.get().getId())
                            .withError(MSG_INVALID_REQUEST),
                    "Oauth2 user not found by username='%s' and organization id='%s'", username, organization.getId()
            );
            return new ModelAndView(
                    "authorize",
                    createResponseBuilder(organization, responseType, clientId, redirectUri, scope, state, null)
                            .put("errorMessage", "Invalid username and password combination.")
                            .build()
            );
        }

        if (!oauthUser.get().isEnabled()) {
            LOGGER.debug("OauthUser is disabled; username='{}' and organization_id='{}'", username, organization.getId());
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.getId())
                            .withClientId(oauthClient.get().getId())
                            .withError(MSG_INVALID_REQUEST),
                    "Oauth2 user is disabled; username='%s' and organization id='%s'", username, organization.getId()
            );
            return new ModelAndView(
                    "authorize",
                    createResponseBuilder(organization, responseType, clientId, redirectUri, scope, state, null)
                            .put("errorMessage", "User access denied")
                            .build()
            );
        }

        if (!organization.getId().equals(oauthUser.get().getOrganizationId())) {
            LOGGER.debug("OauthUser organization_id='{}' does not match request organization_id='{}'", oauthUser.get().getOrganizationId(), organization.getId());
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.getId())
                            .withClientId(oauthClient.get().getId())
                            .withError(MSG_INVALID_REQUEST),
                    "Oauth2 user organization id='%s' does not match request organization id='%s'", oauthUser.get().getOrganizationId(), organization.getId()
            );
            throw new BadRequestException(MSG_INVALID_REQUEST);
        }

        if (!passwordEncoder.matches(password, oauthUser.get().getPassword())) {
            LOGGER.debug("OauthUser password does not match request password");
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.getId())
                            .withClientId(oauthClient.get().getId())
                            .withError(MSG_INVALID_REQUEST),
                    "Oauth2 user password does not match request password"
            );
            return new ModelAndView(
                    "authorize",
                    createResponseBuilder(organization, responseType, clientId, redirectUri, scope, state, null)
                            .put("errorMessage", "Invalid username and password combination.")
                            .build()
            );
        }

        req.getSession().setAttribute(OAUTH2_ATTR_USERNAME, username);
        req.getSession().setAttribute(OAUTH2_ATTR_PASSWORD, password);

        final List<OauthScope> scopeList = scopeService.getScopeListBasedOnRequestedAndAllowed(scopeStr, oauthClient.get());

        if (oauthUser.get().isUsing2Fa()) {

            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withOrganizationId(organization.getId())
                            .withClientId(oauthClient.get().getId())
                            .withDuration(getTimeSinceRequest()),
                    "Displaying Google Authenticator 2FA authorize HTML page"
            );
            return new ModelAndView(
                    "authorize-2fa",
                    createResponseBuilder(organization, responseType, clientId, redirectUri, scope, state, scopeList).build()
            );
        } else {
            if (isNotEmpty(scopeList)) {
                accessLogService.create(
                        AccessLog.builder()
                                .withRequestId(getRequestId())
                                .withOrganizationId(organization.getId())
                                .withClientId(oauthClient.get().getId())
                                .withDuration(getTimeSinceRequest()),
                        "Displaying scopes authorize HTML page"
                );
                return new ModelAndView(
                        "authorize-scopes",
                        createResponseBuilder(organization, responseType, clientId, redirectUri, scope, state, scopeList).build()
                );
            } else {
                return authorizeFinish(req, res, responseType, clientId, redirectUri, state, "");
            }
        }
    }

    @PostMapping("/authorize/2fa")
    @Timed("authorizeUser2Fa")
    public ModelAndView authorizeUser2Fa(final HttpServletRequest req,
                                         final HttpServletResponse res,
                                         @RequestParam(OAUTH2_ATTR_RESPONSE_TYPE) final AuthorizationResponseType responseType,
                                         @RequestParam(OAUTH2_ATTR_CLIENT_ID) final String clientId,
                                         @RequestParam(OAUTH2_ATTR_REDIRECT_URI) final String redirectUri,
                                         @RequestParam(OAUTH2_ATTR_STATE) final String state,
                                         @RequestParam(OAUTH2_ATTR_SCOPE) final String scopeStr,
                                         @RequestParam(OAUTH2_ATTR_2FA_CODE) final String code2fa) {
        accessLogService.create(
                AccessLog.builder()
                        .withRequestId(getRequestId())
                        .withDuration(getTimeSinceRequest()),
                "Starting Oauth2 authorization process (Google Authenticator 2FA code verification)"
        );
        final Organization organization = getOrganization(req);

        final Optional<OauthClient> oauthClient = oauthClientDao.getById(clientId);
        if (oauthClient.isEmpty()) {
            LOGGER.debug("OauthClient not found by id='{}'", clientId);
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withOrganizationId(organization.getId())
                            .withDuration(getTimeSinceRequest())
                            .withError(MSG_INVALID_REQUEST),
                    "Oauth2 client not found by id='%s'", clientId
            );
            throw new BadRequestException(MSG_INVALID_REQUEST);
        }
        if (isEmpty(state)) {
            LOGGER.debug("Authorization state parameter is not provided");
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.getId())
                            .withClientId(oauthClient.get().getId())
                            .withError(MSG_INVALID_REQUEST),
                    "Authorization state parameter is not provided"
            );
            throw new BadRequestException(MSG_INVALID_REQUEST);
        }
        if (!oauthClient.get().getOrganizationId().equals(organization.getId())) {
            LOGGER.debug("OauthClient organization_id='{}' does not match domain prefix specified organization_id='{}'", oauthClient.get().getOrganizationId(), organization.getId());
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.getId())
                            .withClientId(oauthClient.get().getId())
                            .withError(MSG_INVALID_REQUEST),
                    "Oauth2 client organization id='%s' does not match domain prefix specified organization id='%s'", oauthClient.get().getOrganizationId(), organization.getId()
            );
            throw new BadRequestException(MSG_INVALID_REQUEST);
        }
        if (!oauthClient.get().isEnabled()) {
            LOGGER.debug("OauthClient is disabled id='{}'", oauthClient.get().getId());
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.getId())
                            .withClientId(oauthClient.get().getId())
                            .withError(MSG_INVALID_REQUEST),
                    "Oauth2 client is disabled id='%s'", oauthClient.get().getId()
            );
            throw new BadRequestException(MSG_INVALID_REQUEST);
        }

        final String scope = scopeService.getScopeStringBasedOnRequestedAndAllowed(scopeStr, oauthClient.get());

        if (oauthClient.get().getRedirectUrls().stream().filter(redirectUri::startsWith).findAny().isEmpty()) {
            LOGGER.debug("OauthClient approved redirect urls={} does not match requested redirect_url='{}'", oauthClient.get().getRedirectUrls(), redirectUri);
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.getId())
                            .withClientId(oauthClient.get().getId())
                            .withError(MSG_INVALID_REQUEST),
                    "Oauth2 client approved redirect urls=%s does not match requested redirect url='%s'", oauthClient.get().getRedirectUrls().toString(), redirectUri
            );
            return new ModelAndView(
                    "authorize",
                    createResponseBuilder(organization, responseType, clientId, redirectUri, scope, state, null)
                            .put("errorMessage", "Client approved redirect urls do not match requested redirect_url")
                            .build()
            );
        }

        // Fetch user authenticated in previous step
        final String username = (String) req.getSession().getAttribute(OAUTH2_ATTR_USERNAME);
        final Optional<OauthUser> oauthUser = oauthUserDao.getByUsernameAndOrganizationId(username, organization.getId());
        if (oauthUser.isEmpty()) {
            LOGGER.debug("OauthUser not found by username='{}' and organization_id='{}'", username, organization.getId());
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.getId())
                            .withClientId(oauthClient.get().getId())
                            .withError(MSG_INVALID_REQUEST),
                    "Oauth2 user not found by username='%s' and organization id='%s'", username, organization.getId()
            );
            return new ModelAndView(
                    "authorize",
                    createResponseBuilder(organization, responseType, clientId, redirectUri, scope, state, null)
                            .put("errorMessage", "Invalid username and password combination.")
                            .build()
            );
        }

        if (!oauthUser.get().isEnabled()) {
            LOGGER.debug("OauthUser is disabled; username='{}' and organization_id='{}'", username, organization.getId());
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.getId())
                            .withClientId(oauthClient.get().getId())
                            .withError(MSG_INVALID_REQUEST),
                    "Oauth2 user is disabled; username='%s' and organization id='%s'", username, organization.getId()
            );
            return new ModelAndView(
                    "authorize",
                    createResponseBuilder(organization, responseType, clientId, redirectUri, scope, state, null)
                            .put("errorMessage", "User access denied")
                            .build()
            );
        }

        if (!organization.getId().equals(oauthUser.get().getOrganizationId())) {
            LOGGER.debug("OauthUser organization_id='{}' does not match request organization_id='{}'", oauthUser.get().getOrganizationId(), organization.getId());
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.getId())
                            .withClientId(oauthClient.get().getId())
                            .withError(MSG_INVALID_REQUEST),
                    "Oauth2 user organization id='%s' does not match request organization id='%s'", oauthUser.get().getOrganizationId(), organization.getId()
            );
            throw new BadRequestException(MSG_INVALID_REQUEST);
        }

        final List<OauthScope> scopeList = scopeService.getScopeListBasedOnRequestedAndAllowed(scopeStr, oauthClient.get());

        Totp totp = new Totp(oauthUser.get().getSecret());
        if (!totp.verify(code2fa)) {
            req.getSession().removeAttribute(TWO_FACTOR_AUTH_SUCCESS_ATTRIBUTE);
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.getId())
                            .withClientId(oauthClient.get().getId())
                            .withError(MSG_INVALID_REQUEST),
                    "User provided invalid Google Authenticator 2FA verification code"
            );
            return new ModelAndView(
                    "authorize-2fa",
                    createResponseBuilder(organization, responseType, clientId, redirectUri, scope, state, null)
                            .put("errorMessage", "Invalid verification code")
                            .build()
            );
        } else {
            req.getSession().setAttribute(TWO_FACTOR_AUTH_SUCCESS_ATTRIBUTE, TWO_FACTOR_AUTH_SUCCESS_ATTRIBUTE);
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.getId())
                            .withClientId(oauthClient.get().getId()),
                    "Google Authenticator 2FA verification code validated"
            );
        }
        if (isNotEmpty(scopeList)) {
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withOrganizationId(organization.getId())
                            .withClientId(oauthClient.get().getId())
                            .withDuration(getTimeSinceRequest()),
                    "Displaying scopes authorize HTML page"
            );
            return new ModelAndView(
                    "authorize-scopes",
                    createResponseBuilder(organization, responseType, clientId, redirectUri, scope, state, scopeList).build()
            );
        } else {
            return authorizeFinish(req, res, responseType, clientId, redirectUri, state, "");
        }
    }

    @PostMapping("/authorize/finish")
    @Timed("authorizeFinish")
    public ModelAndView authorizeFinish(final HttpServletRequest req,
                                        final HttpServletResponse res,
                                        @RequestParam(OAUTH2_ATTR_RESPONSE_TYPE) final AuthorizationResponseType responseType,
                                        @RequestParam(OAUTH2_ATTR_CLIENT_ID) final String clientId,
                                        @RequestParam(OAUTH2_ATTR_REDIRECT_URI) final String redirectUri,
                                        @RequestParam(OAUTH2_ATTR_STATE) final String state,
                                        @RequestParam(OAUTH2_ATTR_SCOPE) final String scopeStr) {
        accessLogService.create(
                AccessLog.builder()
                        .withRequestId(getRequestId())
                        .withDuration(getTimeSinceRequest()),
                "Starting Oauth2 authorization process (redirect with authorization code)"
        );
        final Organization organization = getOrganization(req);

        final Optional<OauthClient> oauthClient = oauthClientDao.getById(clientId);
        if (oauthClient.isEmpty()) {
            LOGGER.debug("OauthClient not found by id='{}'", clientId);
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.getId())
                            .withError(MSG_INVALID_REQUEST),
                    "Oauth2 client not found by id='%s'", clientId
            );
            throw new BadRequestException(MSG_INVALID_REQUEST);
        }
        if (isEmpty(state)) {
            LOGGER.debug("Authorization state parameter is not provided");
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.getId())
                            .withClientId(oauthClient.get().getId())
                            .withError(MSG_INVALID_REQUEST),
                    "Authorization state parameter is not provided"
            );
            throw new BadRequestException(MSG_INVALID_REQUEST);
        }
        if (!oauthClient.get().getOrganizationId().equals(organization.getId())) {
            LOGGER.debug("OauthClient organization_id='{}' does not match domain prefix specified organization_id='{}'", oauthClient.get().getOrganizationId(), organization.getId());
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.getId())
                            .withClientId(oauthClient.get().getId())
                            .withError(MSG_INVALID_REQUEST),
                    "Oauth2 client organization id='%s' does not match domain prefix specified organization id='%s'", oauthClient.get().getOrganizationId(), organization.getId()
            );
            throw new BadRequestException(MSG_INVALID_REQUEST);
        }
        if (!oauthClient.get().isEnabled()) {
            LOGGER.debug("OauthClient is disabled id='{}'", oauthClient.get().getId());
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.getId())
                            .withClientId(oauthClient.get().getId())
                            .withError(MSG_INVALID_REQUEST),
                    "Oauth2 client is disabled id='%s'", oauthClient.get().getId()
            );
            throw new BadRequestException(MSG_INVALID_REQUEST);
        }

        final String scope;
        if (isEmpty(scopeStr)) {
            scope = scopeStr;
        } else {
            scope = scopeService.getScopeStringBasedOnRequestedAndAllowed(scopeStr, oauthClient.get());
        }

        if (oauthClient.get().getRedirectUrls().stream().filter(redirectUri::startsWith).findAny().isEmpty()) {
            LOGGER.debug("OauthClient approved redirect urls={} does not match requested redirect_url='{}'", oauthClient.get().getRedirectUrls(), redirectUri);
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.getId())
                            .withClientId(oauthClient.get().getId())
                            .withError(MSG_INVALID_REQUEST),
                    "Oauth2 client approved redirect urls=%s does not match requested redirect url='%s'", oauthClient.get().getRedirectUrls().toString(), redirectUri
            );
            return new ModelAndView(
                    "authorize",
                    createResponseBuilder(organization, responseType, clientId, redirectUri, scope, state, null)
                            .put("errorMessage", "Client approved redirect urls do not match requested redirect_url")
                            .build()
            );
        }

        final String username = (String) req.getSession().getAttribute(OAUTH2_ATTR_USERNAME);
        final String password = (String) req.getSession().getAttribute(OAUTH2_ATTR_PASSWORD);

        // Authenticate user credentials
        final Optional<OauthUser> oauthUser = oauthUserDao.getByUsernameAndOrganizationId(username, organization.getId());
        if (oauthUser.isEmpty()) {
            LOGGER.debug("OauthUser not found by username='{}' and organization_id='{}'", username, organization.getId());
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.getId())
                            .withClientId(oauthClient.get().getId())
                            .withError(MSG_INVALID_REQUEST),
                    "Oauth2 user not found by username='%s' and organization id='%s'", username, organization.getId()
            );
            return new ModelAndView(
                    "authorize",
                    createResponseBuilder(organization, responseType, clientId, redirectUri, scope, state, null)
                            .put("errorMessage", "Invalid username and password combination.")
                            .build()
            );
        }

        if (!organization.getId().equals(oauthUser.get().getOrganizationId())) {
            LOGGER.debug("OauthUser organization_id='{}' does not match request organization_id='{}'", oauthUser.get().getOrganizationId(), organization.getId());
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.getId())
                            .withClientId(oauthClient.get().getId())
                            .withError(MSG_INVALID_REQUEST),
                    "Oauth2 user organization id='%s' does not match request organization id='%s'", oauthUser.get().getOrganizationId(), organization.getId()
            );
            throw new BadRequestException(MSG_INVALID_REQUEST);
        }

        if (!passwordEncoder.matches(password, oauthUser.get().getPassword())) {
            LOGGER.debug("OauthUser password does not match request password");
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.getId())
                            .withClientId(oauthClient.get().getId())
                            .withError(MSG_INVALID_REQUEST),
                    "Oauth2 user password does not match request password"
            );
            return new ModelAndView(
                    "authorize",
                    createResponseBuilder(organization, responseType, clientId, redirectUri, scope, state, null)
                            .put("errorMessage", "Invalid username and password combination.")
                            .build()
            );
        }

        if (oauthUser.get().isUsing2Fa() && req.getSession().getAttribute(TWO_FACTOR_AUTH_SUCCESS_ATTRIBUTE) == null) {
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.getId())
                            .withClientId(oauthClient.get().getId())
                            .withError(MSG_INVALID_REQUEST),
                    "2FA verification code session attribute not found"
            );
            throw new BadRequestException("2FA verification code session attribute not found");
        }

        final Instant now = Instant.now(defaultClock);
        final Instant expiration = Instant.now(defaultClock).plusSeconds(60);
        final String code = sha256(UUID.randomUUID().toString());
        final OauthToken oauthToken = new OauthToken(
                UUID.randomUUID().toString(),
                now,
                sha256(code),
                organization.getId(),
                oauthClient.get().getId(),
                expiration,
                SPACE_SPLITTER.splitToList(scope),
                oauthUser.get().getId(),
                AUTHORIZATION_CODE,
                getIp(req),
                NetUtils.getUserAgent(req),
                getRequestId(),
                null
        );
        oauthTokenDao.insert(oauthToken);
        accessLogService.create(
                AccessLog.builder()
                        .withRequestId(getRequestId())
                        .withDuration(getTimeSinceRequest())
                        .withOrganizationId(organization.getId())
                        .withClientId(oauthClient.get().getId()),
                "Oauth2 authorization process finished"
        );
        final String redirectUrl = redirectUri + "?" + OAUTH2_ATTR_CODE + "=" + code + "&state=" + state;
        try {
            res.sendRedirect(redirectUrl);
        } catch (IOException e) {
            LOGGER.error("Unable to send redirect to: {}", redirectUrl);
        }

        return null;
    }

    private ImmutableMap.Builder<String, Object> createResponseBuilder(final Organization organization,
                                                                       final AuthorizationResponseType responseType,
                                                                       final String clientId,
                                                                       final String redirectUri,
                                                                       final String scope,
                                                                       final String state,
                                                                       @Nullable final List<OauthScope> scopeList) {
        final ImmutableMap.Builder<String, Object> builder = new ImmutableMap.Builder<String, Object>()
                .put("organizationId", organization.getId())
                .put("organizationName", organization.getName())
                .put("responseType", responseType)
                .put("clientId", clientId)
                .put("redirectUri", redirectUri)
                .put("scope", scope)
                .put("state", state);

        if (scopeList != null) {
            builder.put("scopeList", scopeList);
        }
        return builder;
    }
}
