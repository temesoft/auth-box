package com.authbox.server.service.processor;

import com.authbox.base.exception.BadRequestException;
import com.authbox.base.exception.Oauth2Exception;
import com.authbox.base.model.AccessLog;
import com.authbox.base.model.GrantType;
import com.authbox.base.model.OauthClient;
import com.authbox.base.model.OauthToken;
import com.authbox.base.model.OauthTokenResponse;
import com.authbox.base.model.OauthUser;
import com.authbox.base.model.Organization;
import com.authbox.server.service.TokenEndpointProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.Optional;

import static com.authbox.base.config.Constants.MSG_INVALID_REQUEST;
import static com.authbox.base.config.Constants.MSG_INVALID_TOKEN;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_REFRESH_TOKEN;
import static com.authbox.base.config.Constants.SPACE;
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

        final String refreshTokenStr = req.getParameter(OAUTH2_ATTR_REFRESH_TOKEN);
        if (isEmpty(refreshTokenStr)) {
            LOGGER.debug("Refresh token is not provided or empty");
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withClientId(oauthClient.getId())
                            .withError(MSG_INVALID_REQUEST)
                            .withOrganizationId(organization.getId()),
                    "Refresh token is not provided or empty"
            );
            throw new BadRequestException(MSG_INVALID_REQUEST);
        }

        final String hash = sha256(refreshTokenStr);
        final Optional<OauthToken> refreshToken = oauthTokenDao.getByHash(hash);
        if (refreshToken.isEmpty()) {
            LOGGER.debug("Refresh token='{}' / hash='{}' not found", refreshTokenStr, hash);
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withClientId(oauthClient.getId())
                            .withError(MSG_INVALID_REQUEST)
                            .withOrganizationId(organization.getId()),
                    "Refresh token hash='%s' not found", hash
            );
            throw new Oauth2Exception(MSG_INVALID_TOKEN);
        }

        if (!refreshToken.get().getTokenType().equals(REFRESH_TOKEN)) {
            LOGGER.debug("Provided token is not ACCESS_TOKEN. type='{}' / hash='{}'", refreshToken.get().getTokenType(), hash);
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withClientId(oauthClient.getId())
                            .withError(MSG_INVALID_REQUEST)
                            .withOrganizationId(organization.getId()),
                    "Provided token is not ACCESS_TOKEN. type='%s' hash='%s'", refreshToken.get().getTokenType().name(), hash
            );
            throw new Oauth2Exception(MSG_INVALID_TOKEN);
        }

        if (!refreshToken.get().getOrganizationId().equals(organization.getId())) {
            LOGGER.debug("Refresh does not belong to organization_id='{}'", organization.getId());
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withClientId(oauthClient.getId())
                            .withError(MSG_INVALID_REQUEST)
                            .withOrganizationId(organization.getId()),
                    "Refresh does not belong to organization id='%s'", organization.getId()
            );
            throw new Oauth2Exception(MSG_INVALID_TOKEN);
        }

        final Instant now = Instant.now(defaultClock);
        if (now.isAfter(refreshToken.get().getExpiration())) {
            LOGGER.debug("Refresh token expired");
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withClientId(oauthClient.getId())
                            .withError(MSG_INVALID_REQUEST)
                            .withOrganizationId(organization.getId()),
                    "Refresh token expired hash='%s'", hash
            );
            throw new Oauth2Exception(MSG_INVALID_TOKEN);
        }

        final Optional<OauthUser> oauthUser = oauthUserDao.getById(refreshToken.get().getOauthUserId());
        if (oauthUser.isEmpty()) {
            LOGGER.debug("OauthUser not found by id='{}'", refreshToken.get().getOauthUserId());
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withClientId(oauthClient.getId())
                            .withError(MSG_INVALID_REQUEST)
                            .withOrganizationId(organization.getId()),
                    "Oauth2 user not found by id='%s'", refreshToken.get().getOauthUserId()
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

        final String scope = join(SPACE, refreshToken.get().getScopes());
        if (oauthClient.getTokenFormat().equals(JWT)) {
            return createJwtAccessToken(organization, oauthClient, scope, getProcessingGrantType(), oauthUser.get(), getIp(req), getUserAgent(req), refreshToken.get().getId());
        } else {
            return createStandardAccessToken(organization, oauthClient, scope, getProcessingGrantType(), oauthUser.get(), getIp(req), getUserAgent(req), refreshToken.get().getId());
        }
    }

    @Override
    public GrantType getProcessingGrantType() {
        return GrantType.refresh_token;
    }
}