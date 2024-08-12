package com.authbox.server.service.processor;

import com.authbox.base.exception.BadRequestException;
import com.authbox.base.exception.Oauth2Exception;
import com.authbox.base.model.AccessLog;
import com.authbox.base.model.GrantType;
import com.authbox.base.model.OauthTokenResponse;
import com.authbox.base.model.Organization;
import com.authbox.server.service.TokenEndpointProcessor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.time.Instant;

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

@Slf4j
public class RefreshTokenGrantTypeTokenEndpointProcessor extends TokenEndpointProcessor {

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

        val refreshTokenStr = req.getParameter(OAUTH2_ATTR_REFRESH_TOKEN);
        if (isEmpty(refreshTokenStr)) {
            log.debug("Refresh token is not provided or empty");
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

        val hash = sha256(refreshTokenStr);
        val refreshToken = oauthTokenDao.getByHash(hash);
        if (refreshToken.isEmpty()) {
            log.debug("Refresh token='{}' / hash='{}' not found", refreshTokenStr, hash);
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
            log.debug("Provided token is not ACCESS_TOKEN. type='{}' / hash='{}'", refreshToken.get().getTokenType(), hash);
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withClientId(oauthClient.getId())
                            .withError(MSG_INVALID_REQUEST)
                            .withOrganizationId(organization.getId()),
                    "Provided token is not ACCESS_TOKEN. type='%s' hash='%s'",
                    refreshToken.get().getTokenType().name(), hash
            );
            throw new Oauth2Exception(MSG_INVALID_TOKEN);
        }

        if (!refreshToken.get().getOrganizationId().equals(organization.getId())) {
            log.debug("Refresh does not belong to organization_id='{}'", organization.getId());
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

        val now = Instant.now(defaultClock);
        if (now.isAfter(refreshToken.get().getExpiration())) {
            log.debug("Refresh token expired");
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

        val oauthUser = oauthUserDao.getById(refreshToken.get().getOauthUserId());
        if (oauthUser.isEmpty()) {
            log.debug("OauthUser not found by id='{}'", refreshToken.get().getOauthUserId());
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

        val scope = join(SPACE, refreshToken.get().getScopes());
        if (oauthClient.getTokenFormat().equals(JWT)) {
            return createJwtAccessToken(organization,
                    oauthClient,
                    scope,
                    getProcessingGrantType(),
                    oauthUser.get(),
                    getIp(req),
                    getUserAgent(req),
                    refreshToken.get().getId());
        } else {
            return createStandardAccessToken(organization,
                    oauthClient,
                    scope,
                    getProcessingGrantType(),
                    oauthUser.get(),
                    getIp(req),
                    getUserAgent(req),
                    refreshToken.get().getId());
        }
    }

    @Override
    public GrantType getProcessingGrantType() {
        return GrantType.refresh_token;
    }
}