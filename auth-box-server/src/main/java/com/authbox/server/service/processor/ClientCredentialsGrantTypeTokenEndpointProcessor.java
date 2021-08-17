package com.authbox.server.service.processor;

import com.authbox.base.model.GrantType;
import com.authbox.base.model.OauthClient;
import com.authbox.base.model.OauthTokenResponse;
import com.authbox.base.model.Organization;
import com.authbox.base.service.AccessLogService;
import com.authbox.server.service.ScopeService;
import com.authbox.server.service.TokenEndpointProcessor;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.authbox.base.config.Constants.OAUTH2_ATTR_SCOPE;
import static com.authbox.base.model.AccessLog.AccessLogBuilder.accessLogBuilder;
import static com.authbox.base.model.TokenFormat.JWT;
import static com.authbox.base.util.NetUtils.getIp;
import static com.authbox.base.util.NetUtils.getUserAgent;
import static com.authbox.server.util.RequestUtils.getRequestId;
import static com.authbox.server.util.RequestUtils.getTimeSinceRequest;

public class ClientCredentialsGrantTypeTokenEndpointProcessor extends TokenEndpointProcessor {

    @Autowired
    protected ScopeService scopeService;

    @Autowired
    protected AccessLogService accessLogService;

    @Override
    public OauthTokenResponse process(final Organization organization, final HttpServletRequest req, final HttpServletResponse res) {
        accessLogService.create(
                accessLogBuilder()
                        .withRequestId(getRequestId())
                        .withDuration(getTimeSinceRequest())
                        .withOrganizationId(organization.id),
                "Parsing and validating Oauth2 client"
        );
        final OauthClient oauthClient = parsingValidationService.getOauthClient(req, organization);

        final String scope = scopeService.getScopeStringBasedOnRequestedAndAllowed(req.getParameter(OAUTH2_ATTR_SCOPE), oauthClient);

        if (oauthClient.tokenFormat.equals(JWT)) {
            accessLogService.create(
                    accessLogBuilder()
                            .withRequestId(getRequestId())
                            .withOrganizationId(organization.id)
                            .withDuration(getTimeSinceRequest())
                            .withClientId(oauthClient.id),
                    "Generating JWT access token"
            );
            return createJwtAccessToken(organization, oauthClient, scope, getProcessingGrantType(), null, getIp(req), getUserAgent(req), null);
        } else {
            accessLogService.create(
                    accessLogBuilder()
                            .withRequestId(getRequestId())
                            .withOrganizationId(organization.id)
                            .withDuration(getTimeSinceRequest())
                            .withClientId(oauthClient.id),
                    "Generating standard access token"
            );
            return createStandardAccessToken(organization, oauthClient, scope, getProcessingGrantType(), null, getIp(req), getUserAgent(req), null);
        }
    }

    @Override
    public GrantType getProcessingGrantType() {
        return GrantType.client_credentials;
    }
}