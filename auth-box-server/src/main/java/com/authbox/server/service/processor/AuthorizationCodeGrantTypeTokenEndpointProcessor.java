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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import java.time.Instant;
import java.util.Optional;

import static com.authbox.base.config.Constants.MSG_INVALID_REQUEST;
import static com.authbox.base.config.Constants.MSG_INVALID_TOKEN;
import static com.authbox.base.config.Constants.MSG_UNAUTHORIZED_REQUEST;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_CODE;
import static com.authbox.base.config.Constants.SPACE;
import static com.authbox.base.model.TokenFormat.JWT;
import static com.authbox.base.model.TokenType.AUTHORIZATION_CODE;
import static com.authbox.base.util.HashUtils.sha256;
import static com.authbox.base.util.NetUtils.getIp;
import static com.authbox.base.util.NetUtils.getUserAgent;
import static com.authbox.server.util.RequestUtils.getRequestId;
import static com.authbox.server.util.RequestUtils.getTimeSinceRequest;
import static java.lang.String.join;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.springframework.util.ObjectUtils.isEmpty;

public class AuthorizationCodeGrantTypeTokenEndpointProcessor extends TokenEndpointProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationCodeGrantTypeTokenEndpointProcessor.class);

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

        final String code = req.getParameter(OAUTH2_ATTR_CODE);
        if (isEmpty(code)) {
            LOGGER.debug("Authorization code is missing or empty");
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.getId())
                            .withClientId(oauthClient.getId())
                            .withError(MSG_INVALID_REQUEST),
                    "Authorization code is missing or empty"
            );
            throw new BadRequestException(MSG_INVALID_REQUEST);
        }

        final String hash = sha256(code);
        final Optional<OauthToken> oauthToken = oauthTokenDao.getByHash(hash);
        if (oauthToken.isEmpty()) {
            LOGGER.debug("Authorization code='{}', hash='{}' was not found", code, hash);
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.getId())
                            .withClientId(oauthClient.getId())
                            .withError(MSG_UNAUTHORIZED_REQUEST),
                    "Authorization code='%s', hash='%s' was not found", code, hash
            );
            throw new Oauth2Exception(MSG_UNAUTHORIZED_REQUEST);
        }

        if (!oauthToken.get().getTokenType().equals(AUTHORIZATION_CODE)) {
            LOGGER.debug("Provided token is not ACCESS_TOKEN. type='{}' token='{}' / hash='{}'", oauthToken.get().getTokenType(), code, hash);
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.getId())
                            .withClientId(oauthClient.getId())
                            .withError(MSG_INVALID_TOKEN),
                    "Provided token is not ACCESS_TOKEN. type='%s' token='%s' / hash='%s'", oauthToken.get().getTokenType().name(), code, hash
            );
            throw new Oauth2Exception(MSG_INVALID_TOKEN);
        }

        if (isNotEmpty(oauthToken.get().getLinkedTokenId())) {
            LOGGER.debug("Provided authorization code token is already used. token='{}' / hash='{}'", code, hash);
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.getId())
                            .withClientId(oauthClient.getId())
                            .withError(MSG_INVALID_TOKEN),
                    "Provided authorization code token is already used. hash='%s'", hash
            );
            throw new Oauth2Exception(MSG_INVALID_TOKEN);
        }

        final Instant now = Instant.now(defaultClock);
        if (now.isAfter(oauthToken.get().getExpiration())) {
            LOGGER.debug("Authorization code expired code='{}', hash='{}'", code, hash);
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.getId())
                            .withClientId(oauthClient.getId())
                            .withError(MSG_INVALID_TOKEN),
                    "Authorization code expired code='%s', hash='%s'", code, hash
            );
            throw new Oauth2Exception(MSG_INVALID_TOKEN);
        }

        if (isEmpty(oauthToken.get().getOauthUserId())) {
            LOGGER.debug("Authorization code user id not available");
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.getId())
                            .withClientId(oauthClient.getId())
                            .withError(MSG_UNAUTHORIZED_REQUEST),
                    "Authorization code user id not available"
            );
            throw new Oauth2Exception(MSG_UNAUTHORIZED_REQUEST);
        }
        final Optional<OauthUser> oauthUser = oauthUserDao.getById(oauthToken.get().getOauthUserId());
        if (oauthUser.isEmpty()) {
            LOGGER.debug("Authorization code user not found by id='{}'", oauthToken.get().getOauthUserId());
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.getId())
                            .withClientId(oauthClient.getId())
                            .withError(MSG_UNAUTHORIZED_REQUEST),
                    "Authorization code user not found by id='%s'", oauthToken.get().getOauthUserId()
            );
            throw new Oauth2Exception(MSG_UNAUTHORIZED_REQUEST);
        }

        if (oauthClient.getTokenFormat().equals(JWT)) {
            return createJwtAccessToken(
                    organization,
                    oauthClient,
                    join(SPACE, oauthToken.get().getScopes()),
                    getProcessingGrantType(),
                    oauthUser.get(),
                    getIp(req),
                    getUserAgent(req),
                    oauthToken.get().getId());
        } else {
            return createStandardAccessToken(
                    organization,
                    oauthClient,
                    join(SPACE, oauthToken.get().getScopes()),
                    getProcessingGrantType(),
                    oauthUser.get(),
                    getIp(req),
                    getUserAgent(req),
                    oauthToken.get().getId());
        }
    }

    @Override
    public GrantType getProcessingGrantType() {
        return GrantType.authorization_code;
    }
}