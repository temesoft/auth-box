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
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
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

@Slf4j
public class AuthorizationCodeGrantTypeTokenEndpointProcessor extends TokenEndpointProcessor {

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

        val code = req.getParameter(OAUTH2_ATTR_CODE);
        if (isEmpty(code)) {
            log.debug("Authorization code is missing or empty");
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

        val hash = sha256(code);
        val oauthToken = oauthTokenDao.getByHash(hash);
        if (oauthToken.isEmpty()) {
            log.debug("Authorization code='{}', hash='{}' was not found", code, hash);
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
            log.debug("Provided token is not ACCESS_TOKEN. type='{}' token='{}' / hash='{}'",
                    oauthToken.get().getTokenType(), code, hash);
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.getId())
                            .withClientId(oauthClient.getId())
                            .withError(MSG_INVALID_TOKEN),
                    "Provided token is not ACCESS_TOKEN. type='%s' token='%s' / hash='%s'",
                    oauthToken.get().getTokenType().name(), code, hash
            );
            throw new Oauth2Exception(MSG_INVALID_TOKEN);
        }

        if (isNotEmpty(oauthToken.get().getLinkedTokenId())) {
            log.debug("Provided authorization code token is already used. token='{}' / hash='{}'", code, hash);
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

        val now = Instant.now(defaultClock);
        if (now.isAfter(oauthToken.get().getExpiration())) {
            log.debug("Authorization code expired code='{}', hash='{}'", code, hash);
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
            log.debug("Authorization code user id not available");
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
        val oauthUser = oauthUserDao.getById(oauthToken.get().getOauthUserId());
        if (oauthUser.isEmpty()) {
            log.debug("Authorization code user not found by id='{}'", oauthToken.get().getOauthUserId());
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