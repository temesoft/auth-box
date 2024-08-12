package com.authbox.server.service.processor;

import com.authbox.base.model.AccessLog;
import com.authbox.base.model.GrantType;
import com.authbox.base.model.OauthClient;
import com.authbox.base.model.OauthTokenResponse;
import com.authbox.base.model.Organization;
import com.authbox.base.service.AccessLogService;
import com.authbox.server.service.ScopeService;
import com.authbox.server.service.TokenEndpointProcessor;
import lombok.AllArgsConstructor;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;

import static com.authbox.base.config.Constants.OAUTH2_ATTR_SCOPE;
import static com.authbox.base.model.TokenFormat.JWT;
import static com.authbox.base.util.NetUtils.getIp;
import static com.authbox.base.util.NetUtils.getUserAgent;
import static com.authbox.server.util.RequestUtils.getRequestId;
import static com.authbox.server.util.RequestUtils.getTimeSinceRequest;

@AllArgsConstructor
public class ClientCredentialsGrantTypeTokenEndpointProcessor extends TokenEndpointProcessor {

    private final ScopeService scopeService;
    private final AccessLogService accessLogService;

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

        if (oauthClient.getTokenFormat().equals(JWT)) {
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withOrganizationId(organization.getId())
                            .withDuration(getTimeSinceRequest())
                            .withClientId(oauthClient.getId()),
                    "Generating JWT access token"
            );
            return createJwtAccessToken(organization,
                    oauthClient,
                    scope,
                    getProcessingGrantType(),
                    null,
                    getIp(req),
                    getUserAgent(req),
                    null);
        } else {
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withOrganizationId(organization.getId())
                            .withDuration(getTimeSinceRequest())
                            .withClientId(oauthClient.getId()),
                    "Generating standard access token"
            );
            return createStandardAccessToken(organization,
                    oauthClient,
                    scope,
                    getProcessingGrantType(),
                    null,
                    getIp(req),
                    getUserAgent(req),
                    null);
        }
    }

    @Override
    public GrantType getProcessingGrantType() {
        return GrantType.client_credentials;
    }
}