package com.authbox.server.service.processor;

import com.authbox.base.exception.BadRequestException;
import com.authbox.base.exception.Oauth2Exception;
import com.authbox.base.model.GrantType;
import com.authbox.base.model.OauthClient;
import com.authbox.base.model.OauthToken;
import com.authbox.base.model.OauthTokenResponse;
import com.authbox.base.model.OauthUser;
import com.authbox.base.model.Organization;
import com.authbox.server.service.TokenEndpointProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.Optional;

import static com.authbox.base.config.Constants.MSG_INVALID_REQUEST;
import static com.authbox.base.config.Constants.MSG_INVALID_TOKEN;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_REFRESH_TOKEN;
import static com.authbox.base.config.Constants.SPACE;
import static com.authbox.base.model.AccessLog.AccessLogBuilder.accessLogBuilder;
import static com.authbox.base.model.TokenFormat.JWT;
import static com.authbox.base.model.TokenType.REFRESH_TOKEN;
import static com.authbox.base.util.HashUtils.sha256;
import static com.authbox.base.util.NetUtils.getIp;
import static com.authbox.base.util.NetUtils.getUserAgent;
import static com.authbox.server.util.RequestUtils.getRequestId;
import static com.authbox.server.util.RequestUtils.getTimeSinceRequest;
import static java.lang.String.join;
import static org.springframework.util.ObjectUtils.isEmpty;

public class RefreshTokenGrantTypeTokenEndpointProcessor extends TokenEndpointProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(RefreshTokenGrantTypeTokenEndpointProcessor.class);

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

        final String refreshTokenStr = req.getParameter(OAUTH2_ATTR_REFRESH_TOKEN);
        if (isEmpty(refreshTokenStr)) {
            LOGGER.debug("Refresh token is not provided or empty");
            accessLogService.create(
                    accessLogBuilder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withClientId(oauthClient.id)
                            .withError(MSG_INVALID_REQUEST)
                            .withOrganizationId(organization.id),
                    "Refresh token is not provided or empty"
            );
            throw new BadRequestException(MSG_INVALID_REQUEST);
        }

        final String hash = sha256(refreshTokenStr);
        final Optional<OauthToken> refreshToken = oauthTokenDao.getByHash(hash);
        if (refreshToken.isEmpty()) {
            LOGGER.debug("Refresh token='{}' / hash='{}' not found", refreshTokenStr, hash);
            accessLogService.create(
                    accessLogBuilder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withClientId(oauthClient.id)
                            .withError(MSG_INVALID_REQUEST)
                            .withOrganizationId(organization.id),
                    "Refresh token hash='%s' not found", hash
            );
            throw new Oauth2Exception(MSG_INVALID_TOKEN);
        }

        if (!refreshToken.get().tokenType.equals(REFRESH_TOKEN)) {
            LOGGER.debug("Provided token is not ACCESS_TOKEN. type='{}' / hash='{}'", refreshToken.get().tokenType, hash);
            accessLogService.create(
                    accessLogBuilder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withClientId(oauthClient.id)
                            .withError(MSG_INVALID_REQUEST)
                            .withOrganizationId(organization.id),
                    "Provided token is not ACCESS_TOKEN. type='%s' hash='%s'", refreshToken.get().tokenType.name(), hash
            );
            throw new Oauth2Exception(MSG_INVALID_TOKEN);
        }

        if (!refreshToken.get().organizationId.equals(organization.id)) {
            LOGGER.debug("Refresh does not belong to organization_id='{}'", organization.id);
            accessLogService.create(
                    accessLogBuilder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withClientId(oauthClient.id)
                            .withError(MSG_INVALID_REQUEST)
                            .withOrganizationId(organization.id),
                    "Refresh does not belong to organization id='%s'", organization.id
            );
            throw new Oauth2Exception(MSG_INVALID_TOKEN);
        }

        final Instant now = Instant.now(defaultClock);
        if (now.isAfter(refreshToken.get().expiration)) {
            LOGGER.debug("Refresh token expired");
            accessLogService.create(
                    accessLogBuilder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withClientId(oauthClient.id)
                            .withError(MSG_INVALID_REQUEST)
                            .withOrganizationId(organization.id),
                    "Refresh token expired hash='%s'", hash
            );
            throw new Oauth2Exception(MSG_INVALID_TOKEN);
        }

        final Optional<OauthUser> oauthUser = oauthUserDao.getById(refreshToken.get().oauthUserId);
        if (oauthUser.isEmpty()) {
            LOGGER.debug("OauthUser not found by id='{}'", refreshToken.get().oauthUserId);
            accessLogService.create(
                    accessLogBuilder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withClientId(oauthClient.id)
                            .withError(MSG_INVALID_REQUEST)
                            .withOrganizationId(organization.id),
                    "Oauth2 user not found by id='%s'", refreshToken.get().oauthUserId
            );
            throw new BadRequestException(MSG_INVALID_REQUEST);
        }

        if (!organization.id.equals(oauthUser.get().organizationId)) {
            LOGGER.debug("OauthUser organization_id='{}' does not match request organization_id='{}'", oauthUser.get().organizationId, organization.id);
            accessLogService.create(
                    accessLogBuilder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withClientId(oauthClient.id)
                            .withError(MSG_INVALID_REQUEST)
                            .withOrganizationId(organization.id),
                    "Oauth2 user organization id='%s' does not match request organization id='%s'", oauthUser.get().organizationId, organization.id
            );
            throw new BadRequestException(MSG_INVALID_REQUEST);
        }

        final String scope = join(SPACE, refreshToken.get().scopes);
        if (oauthClient.tokenFormat.equals(JWT)) {
            return createJwtAccessToken(organization, oauthClient, scope, getProcessingGrantType(), oauthUser.get(), getIp(req), getUserAgent(req), refreshToken.get().id);
        } else {
            return createStandardAccessToken(organization, oauthClient, scope, getProcessingGrantType(), oauthUser.get(), getIp(req), getUserAgent(req), refreshToken.get().id);
        }
    }

    @Override
    public GrantType getProcessingGrantType() {
        return GrantType.refresh_token;
    }
}