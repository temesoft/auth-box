package com.authbox.server.controller;

import com.authbox.base.exception.Oauth2Exception;
import com.authbox.base.model.AccessLog;
import com.authbox.base.model.GrantType;
import com.authbox.base.model.OauthTokenResponse;
import com.authbox.server.service.TokenDetailsService;
import com.authbox.server.service.TokenEndpointProcessor;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.authbox.base.config.Constants.HEADER_AUTHORIZATION;
import static com.authbox.base.config.Constants.HEADER_AUTHORIZATION_PREFIX_BEARER;
import static com.authbox.base.config.Constants.MSG_INVALID_GRANT_TYPE;
import static com.authbox.base.config.Constants.MSG_UNAUTHORIZED_REQUEST;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_ACCESS_TOKEN;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_CODE;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_GRANT_TYPE;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_PASSWORD;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_SCOPE;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_USERNAME;
import static com.authbox.base.config.Constants.OAUTH_PREFIX;
import static com.authbox.server.util.RequestUtils.getRequestId;
import static com.authbox.server.util.RequestUtils.getTimeSinceRequest;
import static org.springframework.util.ObjectUtils.isEmpty;

@RestController
@RequestMapping(OAUTH_PREFIX)
public class Oauth2TokenController extends BaseController {

    @Autowired
    private List<TokenEndpointProcessor> tokenEndpointProcessors;
    @Autowired
    private TokenDetailsService tokenDetailsService;

    @PostMapping("/token")
    @Timed("generateToken")
    @Parameters(
            {
                    @Parameter(name = OAUTH2_ATTR_GRANT_TYPE, required = true,
                            description = "OAuth2 grant type",
                            schema = @Schema(implementation = GrantType.class)),
                    @Parameter(name = OAUTH2_ATTR_SCOPE, description = "OAuth2 scopes (space separated values)"),
                    @Parameter(name = OAUTH2_ATTR_CODE, description = "OAuth2 authorization code"),
                    @Parameter(name = OAUTH2_ATTR_USERNAME, description = "Username value"),
                    @Parameter(name = OAUTH2_ATTR_PASSWORD, description = "Password value")
            }
    )
    public OauthTokenResponse generateToken(final HttpServletRequest req,
                                            final HttpServletResponse res,
                                            @RequestParam(OAUTH2_ATTR_GRANT_TYPE) final String grantTypeStr) {
        accessLogService.create(
                AccessLog.builder()
                        .withRequestId(getRequestId())
                        .withDuration(getTimeSinceRequest()),
                "Starting Oauth2 token generation (grant_type: '%s')",
                grantTypeStr
        );
        val organization = getOrganization(req);

        if (!GrantType.contains(grantTypeStr)) {
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.getId())
                            .withError(MSG_INVALID_GRANT_TYPE),
                    "Oauth2 grant type provided is not valid. grant type='%s'", grantTypeStr
            );
            throw new Oauth2Exception(MSG_INVALID_GRANT_TYPE);
        }

        val grantType = GrantType.valueOf(grantTypeStr);
        val tokenEndpointProcessor = tokenEndpointProcessors
                .stream()
                .filter(processor -> grantType.equals(processor.getProcessingGrantType()))
                .findFirst();

        if (tokenEndpointProcessor.isEmpty()) {
            throw new Oauth2Exception("processor not found");
        }

        return tokenEndpointProcessor.get().process(organization, req, res);
    }

    @PostMapping("/introspection")
    @GetMapping("/introspection")
    @Timed("introspectToken")
    public Map<String, Object> introspectToken(
            final HttpServletRequest req,
            @RequestParam(value = OAUTH2_ATTR_ACCESS_TOKEN, required = false) final String token) {
        accessLogService.create(
                AccessLog.builder()
                        .withRequestId(getRequestId())
                        .withDuration(getTimeSinceRequest()),
                "Starting Oauth2 token introspection"
        );
        val organization = getOrganization(req);
        Optional<String> accessToken = Optional.ofNullable(token);
        if (accessToken.isEmpty()) {
            accessToken = Optional.ofNullable(req.getParameter("token"));
        }
        if (accessToken.isEmpty()) {
            val authHeader = req.getHeader(HEADER_AUTHORIZATION);
            if (!isEmpty(authHeader)) {
                accessToken = Optional.of(authHeader.replace(HEADER_AUTHORIZATION_PREFIX_BEARER, ""));
            }
        }
        if (accessToken.isEmpty()) {
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withOrganizationId(organization.getId())
                            .withDuration(getTimeSinceRequest())
                            .withError(MSG_UNAUTHORIZED_REQUEST),
                    "Oauth2 access token is not provided"
            );
            throw new Oauth2Exception(MSG_UNAUTHORIZED_REQUEST);
        }

        if (!appProperties.isAllowTokenDetailsWithoutClientCredentials()) {
            // validates actual OauthClient
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.getId()),
                    "Parsing and validating Oauth2 client"
            );
            val oauthClient = parsingValidationService.getOauthClient(req, organization);
            return tokenDetailsService.getAccessTokenDetails(organization, accessToken.get(), oauthClient);
        } else {
            return tokenDetailsService.getAccessTokenDetails(organization, accessToken.get(), null);
        }
    }
}
