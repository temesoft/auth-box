package com.authbox.server.service.processor;

import com.authbox.base.exception.BadRequestException;
import com.authbox.base.model.AccessLog;
import com.authbox.base.model.GrantType;
import com.authbox.base.model.OauthClient;
import com.authbox.base.model.OauthTokenResponse;
import com.authbox.base.model.OauthUser;
import com.authbox.base.model.Organization;
import com.authbox.server.service.ScopeService;
import com.authbox.server.service.TokenEndpointProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import java.util.Optional;

import static com.authbox.base.config.Constants.MSG_INVALID_REQUEST;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_PASSWORD;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_SCOPE;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_USERNAME;
import static com.authbox.base.model.TokenFormat.JWT;
import static com.authbox.base.util.NetUtils.getIp;
import static com.authbox.base.util.NetUtils.getUserAgent;
import static com.authbox.server.util.RequestUtils.getRequestId;
import static com.authbox.server.util.RequestUtils.getTimeSinceRequest;

public class PasswordGrantTypeTokenEndpointProcessor extends TokenEndpointProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordGrantTypeTokenEndpointProcessor.class);

    @Autowired
    private ScopeService scopeService;

    @Override
    @Transactional
    public OauthTokenResponse process(final Organization organization, final HttpServletRequest req, final HttpServletResponse res) {
        accessLogService.create(
                AccessLog.builder()
                        .withRequestId(getRequestId())
                        .withDuration(getTimeSinceRequest())
                        .withOrganizationId(organization.getId()),
                "Parsing and validating Oauth2 client"
        );
        final OauthClient oauthClient = parsingValidationService.getOauthClient(req, organization);

        final String scope = scopeService.getScopeStringBasedOnRequestedAndAllowed(req.getParameter(OAUTH2_ATTR_SCOPE), oauthClient);

        final Optional<Pair<String, String>> oauthUserCredentials = parsingValidationService.getCredentialsFromParameters(req, OAUTH2_ATTR_USERNAME, OAUTH2_ATTR_PASSWORD);
        if (oauthUserCredentials.isEmpty()) {
            LOGGER.debug("Request does not contain username and/or password");
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withClientId(oauthClient.getId())
                            .withError(MSG_INVALID_REQUEST)
                            .withOrganizationId(organization.getId()),
                    "Request does not contain username and/or password"
            );
            throw new BadRequestException(MSG_INVALID_REQUEST);
        }
        final Optional<OauthUser> oauthUser = oauthUserDao.getByUsernameAndOrganizationId(oauthUserCredentials.get().getFirst(), organization.getId());
        if (oauthUser.isEmpty()) {
            LOGGER.debug("OauthUser not found by username='{}' and organization_id='{}'", oauthUserCredentials.get().getFirst(), organization.getId());
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withClientId(oauthClient.getId())
                            .withError(MSG_INVALID_REQUEST)
                            .withOrganizationId(organization.getId()),
                    "Oauth2 user not found by username='%s' and organization id='%s'", oauthUserCredentials.get().getFirst(), organization.getId()
            );
            throw new BadRequestException(MSG_INVALID_REQUEST);
        }

        if (!organization.getId().equals(oauthUser.get().getOrganizationId())) {
            LOGGER.debug("OauthUser organization_id='{}' does not match request organization_id='{}'", oauthUser.get().getOrganizationId(), organization.getId());
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withClientId(oauthClient.getId())
                            .withError(MSG_INVALID_REQUEST)
                            .withOrganizationId(organization.getId()),
                    "Oauth2 user organization id='%s' does not match request organization id='%s'", oauthUser.get().getOrganizationId(), organization.getId()
            );
            throw new BadRequestException(MSG_INVALID_REQUEST);
        }

        if (!passwordEncoder.matches(oauthUserCredentials.get().getSecond(), oauthUser.get().getPassword())) {
            LOGGER.debug("OauthUser password does not match request password");
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withClientId(oauthClient.getId())
                            .withError(MSG_INVALID_REQUEST)
                            .withOrganizationId(organization.getId()),
                    "Oauth2 user password does not match request password"
            );
            throw new BadRequestException(MSG_INVALID_REQUEST);
        }

        if (oauthClient.getTokenFormat().equals(JWT)) {
            return createJwtAccessToken(organization, oauthClient, scope, getProcessingGrantType(), oauthUser.get(), getIp(req), getUserAgent(req), null);
        } else {
            return createStandardAccessToken(organization, oauthClient, scope, getProcessingGrantType(), oauthUser.get(), getIp(req), getUserAgent(req), null);
        }
    }

    @Override
    public GrantType getProcessingGrantType() {
        return GrantType.password;
    }
}
