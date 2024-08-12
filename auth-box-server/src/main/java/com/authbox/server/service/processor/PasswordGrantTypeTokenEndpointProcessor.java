package com.authbox.server.service.processor;

import com.authbox.base.exception.BadRequestException;
import com.authbox.base.model.AccessLog;
import com.authbox.base.model.GrantType;
import com.authbox.base.model.OauthTokenResponse;
import com.authbox.base.model.Organization;
import com.authbox.server.service.ScopeService;
import com.authbox.server.service.TokenEndpointProcessor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import static com.authbox.base.config.Constants.MSG_INVALID_REQUEST;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_PASSWORD;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_SCOPE;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_USERNAME;
import static com.authbox.base.model.TokenFormat.JWT;
import static com.authbox.base.util.NetUtils.getIp;
import static com.authbox.base.util.NetUtils.getUserAgent;
import static com.authbox.server.util.RequestUtils.getRequestId;
import static com.authbox.server.util.RequestUtils.getTimeSinceRequest;

@AllArgsConstructor
@Slf4j
public class PasswordGrantTypeTokenEndpointProcessor extends TokenEndpointProcessor {

    private final ScopeService scopeService;

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
        val oauthClient = parsingValidationService.getOauthClient(req, organization);

        val scope = scopeService.getScopeStringBasedOnRequestedAndAllowed(req.getParameter(OAUTH2_ATTR_SCOPE), oauthClient);

        val oauthUserCredentials = parsingValidationService.getCredentialsFromParameters(req, OAUTH2_ATTR_USERNAME, OAUTH2_ATTR_PASSWORD);
        if (oauthUserCredentials.isEmpty()) {
            log.debug("Request does not contain username and/or password");
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
        val oauthUser = oauthUserDao.getByUsernameAndOrganizationId(oauthUserCredentials.get().getFirst(), organization.getId());
        if (oauthUser.isEmpty()) {
            log.debug("OauthUser not found by username='{}' and organization_id='{}'", oauthUserCredentials.get().getFirst(), organization.getId());
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withClientId(oauthClient.getId())
                            .withError(MSG_INVALID_REQUEST)
                            .withOrganizationId(organization.getId()),
                    "Oauth2 user not found by username='%s' and organization id='%s'",
                    oauthUserCredentials.get().getFirst(), organization.getId()
            );
            throw new BadRequestException(MSG_INVALID_REQUEST);
        }

        if (!organization.getId().equals(oauthUser.get().getOrganizationId())) {
            log.debug("OauthUser organization_id='{}' does not match request organization_id='{}'",
                    oauthUser.get().getOrganizationId(), organization.getId());
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withClientId(oauthClient.getId())
                            .withError(MSG_INVALID_REQUEST)
                            .withOrganizationId(organization.getId()),
                    "Oauth2 user organization id='%s' does not match request organization id='%s'",
                    oauthUser.get().getOrganizationId(), organization.getId()
            );
            throw new BadRequestException(MSG_INVALID_REQUEST);
        }

        if (!passwordEncoder.matches(oauthUserCredentials.get().getSecond(), oauthUser.get().getPassword())) {
            log.debug("OauthUser password does not match request password");
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
            return createJwtAccessToken(organization,
                    oauthClient,
                    scope,
                    getProcessingGrantType(),
                    oauthUser.get(),
                    getIp(req),
                    getUserAgent(req),
                    null);
        } else {
            return createStandardAccessToken(organization,
                    oauthClient,
                    scope,
                    getProcessingGrantType(),
                    oauthUser.get(),
                    getIp(req),
                    getUserAgent(req),
                    null);
        }
    }

    @Override
    public GrantType getProcessingGrantType() {
        return GrantType.password;
    }
}
