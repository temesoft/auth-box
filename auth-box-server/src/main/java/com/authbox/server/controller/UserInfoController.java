package com.authbox.server.controller;

import com.authbox.base.dao.OauthClientDao;
import com.authbox.base.dao.OauthTokenDao;
import com.authbox.base.dao.OauthUserDao;
import com.authbox.base.exception.Oauth2Exception;
import com.authbox.base.model.OauthClient;
import com.authbox.base.model.OauthToken;
import com.authbox.base.model.OauthUser;
import com.authbox.base.model.Organization;
import com.authbox.server.service.ParsingValidationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Optional;

import static com.authbox.base.config.Constants.HEADER_AUTHORIZATION;
import static com.authbox.base.config.Constants.HEADER_AUTHORIZATION_PREFIX_BEARER;
import static com.authbox.base.config.Constants.MSG_INVALID_TOKEN;
import static com.authbox.base.config.Constants.MSG_UNAUTHORIZED_REQUEST;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_ACCESS_TOKEN;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_METADATA;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_ORGANIZATION_ID;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_USERNAME;
import static com.authbox.base.config.Constants.OAUTH_PREFIX;
import static com.authbox.base.model.AccessLog.AccessLogBuilder.accessLogBuilder;
import static com.authbox.base.model.TokenType.ACCESS_TOKEN;
import static com.authbox.base.util.HashUtils.sha256;
import static com.authbox.server.util.RequestUtils.getRequestId;
import static com.authbox.server.util.RequestUtils.getTimeSinceRequest;
import static org.springframework.util.ObjectUtils.isEmpty;

@RestController
@RequestMapping(OAUTH_PREFIX)
public class UserInfoController extends BaseController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserInfoController.class);

    private final OauthTokenDao oauthTokenDao;
    private final OauthUserDao oauthUserDao;
    private final OauthClientDao oauthClientDao;
    private final ObjectMapper objectMapper;
    private final ParsingValidationService parsingValidationService;

    public UserInfoController(final OauthTokenDao oauthTokenDao, final OauthUserDao oauthUserDao, final OauthClientDao oauthClientDao, final ObjectMapper objectMapper, final ParsingValidationService parsingValidationService) {
        this.oauthTokenDao = oauthTokenDao;
        this.oauthUserDao = oauthUserDao;
        this.oauthClientDao = oauthClientDao;
        this.objectMapper = objectMapper;
        this.parsingValidationService = parsingValidationService;
    }

    @GetMapping("/user")
    @Timed("getUserInfo")
    public Map<String, Object> getUserInfo(final HttpServletRequest req,
                                           @RequestParam(value = OAUTH2_ATTR_ACCESS_TOKEN, required = false) final String token) {
        accessLogService.create(
                accessLogBuilder()
                        .withRequestId(getRequestId())
                        .withDuration(getTimeSinceRequest()),
                "Processing user info request"
        );
        final Organization organization = getOrganization(req);

        Optional<String> accessToken = Optional.ofNullable(token);
        if (accessToken.isEmpty()) {
            accessToken = Optional.ofNullable(req.getParameter("token"));
        }
        if (accessToken.isEmpty()) {
            final String authHeader = req.getHeader(HEADER_AUTHORIZATION);
            if (!isEmpty(authHeader)) {
                accessToken = Optional.of(authHeader.replace(HEADER_AUTHORIZATION_PREFIX_BEARER, ""));
            }
        }
        if (accessToken.isEmpty()) {
            accessLogService.create(
                    accessLogBuilder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.id)
                            .withError(MSG_UNAUTHORIZED_REQUEST),
                    "Access token is not provided"
            );
            throw new Oauth2Exception(MSG_UNAUTHORIZED_REQUEST);
        }

        if (!appProperties.isAllowTokenDetailsWithoutClientCredentials()) {
            // validates actual OauthClient
            parsingValidationService.getOauthClient(req, organization);
        }

        final Optional<OauthToken> accessOauthToken = oauthTokenDao.getByHash(sha256(accessToken.get()));
        if (accessOauthToken.isEmpty()) {
            LOGGER.debug("Access token='{}' / hash='{}' not found", accessToken.get(), sha256(accessToken.get()));
            accessLogService.create(
                    accessLogBuilder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.id)
                            .withError(MSG_INVALID_TOKEN),
                    "Access token='%s' / hash='%s' not found", accessToken.get(), sha256(accessToken.get())
            );
            throw new Oauth2Exception(MSG_INVALID_TOKEN);
        }
        if (!accessOauthToken.get().tokenType.equals(ACCESS_TOKEN)) {
            LOGGER.debug("Provided token is not ACCESS_TOKEN. type='{}' token='{}' / hash='{}'", accessOauthToken.get().tokenType, accessToken.get(), sha256(accessToken.get()));
            accessLogService.create(
                    accessLogBuilder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.id)
                            .withClientId(accessOauthToken.get().clientId)
                            .withError(MSG_INVALID_TOKEN),
                    "Provided token is not ACCESS_TOKEN. type='%s' token='%s' / hash='%s'", accessOauthToken.get().tokenType.name(), accessToken.get(), sha256(accessToken.get())
            );
            throw new Oauth2Exception(MSG_INVALID_TOKEN);
        }
        if (!accessOauthToken.get().organizationId.equals(organization.id)) {
            LOGGER.debug("Token does not belong to organization_id='{}'", organization.id);
            accessLogService.create(
                    accessLogBuilder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.id)
                            .withClientId(accessOauthToken.get().clientId)
                            .withError(MSG_INVALID_TOKEN),
                    "Token does not belong to organization id='%s'", organization.id
            );
            throw new Oauth2Exception(MSG_INVALID_TOKEN);
        }

        if (isEmpty(accessOauthToken.get().oauthUserId)) {
            LOGGER.debug("Access token='{}' / hash='{}' is not linked to a oauth user", accessToken.get(), sha256(accessToken.get()));
            accessLogService.create(
                    accessLogBuilder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.id)
                            .withClientId(accessOauthToken.get().clientId)
                            .withError(MSG_INVALID_TOKEN),
                    "Access token='%s' / hash='%s' is not linked to a oauth user", accessToken.get(), sha256(accessToken.get())
            );
            throw new Oauth2Exception(MSG_INVALID_TOKEN);
        }

        final Optional<OauthUser> oauthUser = oauthUserDao.getById(accessOauthToken.get().oauthUserId);
        if (oauthUser.isEmpty()) {
            LOGGER.debug("OauthUser not found by id='{}'", accessOauthToken.get().oauthUserId);
            accessLogService.create(
                    accessLogBuilder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.id)
                            .withClientId(accessOauthToken.get().clientId)
                            .withError(MSG_UNAUTHORIZED_REQUEST),
                    "Oauth2 user not found by id='%s'", accessOauthToken.get().oauthUserId
            );
            throw new Oauth2Exception(MSG_UNAUTHORIZED_REQUEST);
        }

        if (!oauthUser.get().enabled) {
            LOGGER.debug("OauthUser user disabled. id='{}'", oauthUser.get().id);
            accessLogService.create(
                    accessLogBuilder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.id)
                            .withClientId(accessOauthToken.get().clientId)
                            .withError(MSG_UNAUTHORIZED_REQUEST),
                    "Oauth2 user user is disabled. id='%s'", oauthUser.get().id
            );
            throw new Oauth2Exception(MSG_UNAUTHORIZED_REQUEST);
        }

        final Optional<OauthClient> oauthClient = oauthClientDao.getById(accessOauthToken.get().clientId);
        if (oauthClient.isEmpty()) {
            LOGGER.debug("OauthClient not found by id='{}'", accessOauthToken.get().clientId);
            accessLogService.create(
                    accessLogBuilder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.id)
                            .withClientId(accessOauthToken.get().clientId)
                            .withError(MSG_UNAUTHORIZED_REQUEST),
                    "Oauth2 client not found by id='%s'", accessOauthToken.get().clientId
            );
            throw new Oauth2Exception(MSG_UNAUTHORIZED_REQUEST);
        }

        if (!oauthClient.get().enabled) {
            LOGGER.debug("OauthClient is disabled. id='{}'", oauthClient.get().id);
            accessLogService.create(
                    accessLogBuilder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.id)
                            .withClientId(oauthClient.get().id)
                            .withError(MSG_UNAUTHORIZED_REQUEST),
                    "Oauth2 user user disabled. id='%s'", oauthUser.get().id
            );
            throw new Oauth2Exception(MSG_UNAUTHORIZED_REQUEST);
        }

        Object metadata = null;
        try {
            metadata = objectMapper.readValue(oauthUser.get().metadata, Map.class);
        } catch (JsonProcessingException e) {
            LOGGER.debug("Unable to parse metadata for OauthUser user_id='{}'", oauthUser.get().id);
        }

        accessLogService.create(
                accessLogBuilder()
                        .withRequestId(getRequestId())
                        .withDuration(getTimeSinceRequest())
                        .withOrganizationId(organization.id)
                        .withClientId(oauthClient.get().id),
                "User info request finished"
        );

        return ImmutableMap.of(
                "id", oauthUser.get().id,
                OAUTH2_ATTR_USERNAME, oauthUser.get().username,
                OAUTH2_ATTR_ORGANIZATION_ID, oauthUser.get().organizationId,
                OAUTH2_ATTR_METADATA, metadata != null ? metadata : oauthUser.get().metadata
        );
    }
}
