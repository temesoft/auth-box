package com.authbox.server.service;

import com.authbox.base.dao.OauthClientDao;
import com.authbox.base.dao.OauthTokenDao;
import com.authbox.base.dao.OauthUserDao;
import com.authbox.base.exception.BadRequestException;
import com.authbox.base.exception.Oauth2Exception;
import com.authbox.base.model.AccessLog;
import com.authbox.base.model.OauthClient;
import com.authbox.base.model.OauthToken;
import com.authbox.base.model.OauthUser;
import com.authbox.base.model.Organization;
import com.authbox.base.service.AccessLogService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.impl.DefaultClaims;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.security.PublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static com.authbox.base.config.Constants.MSG_INVALID_REQUEST;
import static com.authbox.base.config.Constants.MSG_UNAUTHORIZED_REQUEST;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_ACTIVE;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_CLIENT_ID;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_EXPIRES;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_EXPIRES_IN;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_METADATA;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_ORGANIZATION_ID;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_SCOPE;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_USERNAME;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_USER_ID;
import static com.authbox.base.config.Constants.PERIOD;
import static com.authbox.base.config.Constants.SPACE;
import static com.authbox.base.model.TokenType.ACCESS_TOKEN;
import static com.authbox.base.util.CertificateKeysUtils.generatePublicKey;
import static com.authbox.base.util.HashUtils.sha256;
import static com.authbox.server.util.RequestUtils.getRequestId;
import static com.authbox.server.util.RequestUtils.getTimeSinceRequest;
import static java.lang.String.join;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.springframework.util.ObjectUtils.isEmpty;

@AllArgsConstructor
@Slf4j
public class TokenDetailsServiceImpl implements TokenDetailsService {

    private final OauthTokenDao oauthTokenDao;
    private final OauthUserDao oauthUserDao;
    private final OauthClientDao oauthClientDao;
    private final Clock defaultClock;
    private final ObjectMapper objectMapper;
    private final AccessLogService accessLogService;

    @Override
    public Map<String, Object> getAccessTokenDetails(final Organization organization, final String accessToken, @Nullable final OauthClient providedClient) {
        accessLogService.create(
                AccessLog.builder()
                        .withRequestId(getRequestId())
                        .withDuration(getTimeSinceRequest())
                        .withOrganizationId(organization.getId()),
                "Validating Oauth2 access token details"
        );
        val hash = sha256(accessToken);
        val oauthToken = oauthTokenDao.getByHash(hash);
        if (oauthToken.isEmpty()) {
            log.debug("Unable to find OauthToken by access_token='{}', hash='{}'", accessToken, hash);
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.getId())
                            .withError(MSG_UNAUTHORIZED_REQUEST),
                    "Unable to find Oauth2 token by hash='%s'", hash
            );
            throw new Oauth2Exception(MSG_UNAUTHORIZED_REQUEST);
        }
        if (oauthToken.get().getTokenType() != ACCESS_TOKEN) {
            log.debug("OauthToken is not ACCESS_TOKEN type. access_token='{}', hash='{}'", accessToken, hash);
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.getId())
                            .withOauthTokenId(oauthToken.get().getId())
                            .withError(MSG_UNAUTHORIZED_REQUEST),
                    "Oauth2 token is not ACCESS_TOKEN type. hash='%s'", hash
            );
            throw new Oauth2Exception(MSG_UNAUTHORIZED_REQUEST);
        }
        if (!organization.getId().equals(oauthToken.get().getOrganizationId())) {
            log.debug("OauthToken organization_id='{}' does not match OauthClient specified organization_id='{}'",
                    oauthToken.get().getOrganizationId(), organization.getId());
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.getId())
                            .withError(MSG_UNAUTHORIZED_REQUEST),
                    "Oauth2 token organization id='%s' does not match Oauth2 client specified organization id='%s'",
                    oauthToken.get().getOrganizationId(), organization.getId()
            );
            throw new Oauth2Exception(MSG_UNAUTHORIZED_REQUEST);
        }

        accessLogService.create(
                AccessLog.builder()
                        .withRequestId(getRequestId())
                        .withDuration(getTimeSinceRequest())
                        .withOauthTokenId(oauthToken.get().getId())
                        .withOrganizationId(organization.getId()),
                "Oauth2 token validated"
        );

        val oauthClient = oauthClientDao.getById(oauthToken.get().getClientId());
        if (oauthClient.isEmpty()) {
            log.debug("Unable to find OauthClient by client_id='{}'", oauthToken.get().getClientId());
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.getId())
                            .withError(MSG_UNAUTHORIZED_REQUEST),
                    "Unable to find Oauth2 client by client id='%s'", oauthToken.get().getClientId()
            );
            throw new Oauth2Exception(MSG_UNAUTHORIZED_REQUEST);
        }
        if (!oauthClient.get().isEnabled()) {
            log.debug("OauthClient is disabled. client_id='{}'", oauthToken.get().getClientId());
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.getId())
                            .withClientId(oauthClient.get().getId())
                            .withError(MSG_UNAUTHORIZED_REQUEST),
                    "Oauth2 client is disabled. client id='%s'", oauthToken.get().getClientId()
            );
            throw new Oauth2Exception(MSG_UNAUTHORIZED_REQUEST);
        }

        if (providedClient != null && !providedClient.getId().equals(oauthToken.get().getClientId())) {
            log.debug("OauthClient provided (client_id: {}) does not correspond to OauthClient associated with provided token (client_id: {})", providedClient.getId(), oauthToken.get().getClientId());
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.getId())
                            .withClientId(oauthClient.get().getId())
                            .withError(MSG_UNAUTHORIZED_REQUEST),
                    "Oauth2 client provided (client id: %s) does not correspond to Oauth2 client associated with provided token (client id: %s)", providedClient.getId(), oauthToken.get().getClientId()
            );
            throw new Oauth2Exception(MSG_UNAUTHORIZED_REQUEST);
        }

        if (!organization.getId().equals(oauthClient.get().getOrganizationId())) {
            log.debug("OauthClient organization_id='{}' does not match OauthClient specified organization_id='{}'",
                    oauthToken.get().getOrganizationId(), organization.getId());
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.getId())
                            .withClientId(oauthClient.get().getId())
                            .withError(MSG_UNAUTHORIZED_REQUEST),
                    "Oauth2 client organization id='%s' does not match Oauth2 client specified organization id='%s'",
                    oauthToken.get().getOrganizationId(), organization.getId()
            );
            throw new Oauth2Exception(MSG_UNAUTHORIZED_REQUEST);
        }

        if (accessToken.indexOf(PERIOD) > 0 && accessToken.lastIndexOf(PERIOD) > accessToken.indexOf(PERIOD)) {
            // JWT token format
            return getJwtAccessTokenDetails(oauthClient.get(), oauthToken.get(), accessToken);
        } else {
            // STANDARD token format
            return getStandardAccessTokenDetails(oauthToken.get(), oauthClient.get());
        }
    }

    private Map<String, Object> getJwtAccessTokenDetails(final OauthClient oauthClient, final OauthToken oauthToken, final String accessToken) {
        if (isEmpty(oauthClient.getPublicKey())) {
            log.debug("Client with oauth_client_id='{}' does not have public key to validate JWT token.", oauthClient.getId());
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(oauthClient.getOrganizationId())
                            .withClientId(oauthClient.getId())
                            .withOauthTokenId(oauthToken.getId())
                            .withError(MSG_INVALID_REQUEST),
                    "Client with oauth_client_id='{}' does not have public key to validate JWT token.", oauthClient.getId()
            );
            throw new BadRequestException(MSG_INVALID_REQUEST);
        }

        final PublicKey publicKey;
        try {
            publicKey = generatePublicKey(oauthClient.getPublicKey());
        } catch (IllegalArgumentException e) {
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(oauthClient.getOrganizationId())
                            .withClientId(oauthClient.getId())
                            .withError(MSG_UNAUTHORIZED_REQUEST),
                    e.getMessage()
            );
            throw new BadRequestException(e.getMessage());
        }

        val result = ImmutableMap.<String, Object>builder();
        final Jws<Claims> jws;
        try {
            jws = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(accessToken);
        } catch (MalformedJwtException e) {
            log.debug("Unable to parse JWT token: {}", e.getMessage());
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(oauthClient.getOrganizationId())
                            .withClientId(oauthClient.getId())
                            .withOauthTokenId(oauthToken.getId())
                            .withError(MSG_UNAUTHORIZED_REQUEST),
                    "Unable to parse JWT token: %s", e.getMessage()
            );
            throw new Oauth2Exception(MSG_UNAUTHORIZED_REQUEST);
        } catch (ExpiredJwtException e) {
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(oauthClient.getOrganizationId())
                            .withClientId(oauthClient.getId())
                            .withOauthTokenId(oauthToken.getId())
                            .withError(MSG_UNAUTHORIZED_REQUEST),
                    "Oauth2 JWT token is expired"
            );
            return result.put(OAUTH2_ATTR_ACTIVE, false).build();
        } catch (SignatureException e) {
            log.debug("Unable to verify JWT signature: {}", e.getMessage());
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(oauthClient.getOrganizationId())
                            .withClientId(oauthClient.getId())
                            .withOauthTokenId(oauthToken.getId())
                            .withError(MSG_UNAUTHORIZED_REQUEST),
                    "Unable to verify JWT signature: %s", e.getMessage()
            );
            throw new Oauth2Exception(MSG_UNAUTHORIZED_REQUEST);
        }

        accessLogService.create(
                AccessLog.builder()
                        .withRequestId(getRequestId())
                        .withDuration(getTimeSinceRequest())
                        .withOrganizationId(oauthClient.getOrganizationId())
                        .withClientId(oauthClient.getId())
                        .withOauthTokenId(oauthToken.getId()),
                "Successfully validated JWT token and signature"
        );

        val defaultClaims = (DefaultClaims) jws.getPayload();
        val now = Instant.now(defaultClock);
        result.put(OAUTH2_ATTR_ACTIVE, true);
        result.put(OAUTH2_ATTR_EXPIRES_IN, Duration.between(now, defaultClaims.getExpiration().toInstant()).toSeconds());
        result.put(OAUTH2_ATTR_EXPIRES, defaultClaims.getExpiration().getTime() / 1000);
        result.put(OAUTH2_ATTR_SCOPE, defaultClaims.get(OAUTH2_ATTR_SCOPE));
        val organizationId = (String) defaultClaims.get(OAUTH2_ATTR_ORGANIZATION_ID);
        result.put(OAUTH2_ATTR_ORGANIZATION_ID, organizationId);

        if (!oauthToken.getOrganizationId().equals(organizationId)) {
            log.debug("OauthToken organization_id='{}' does not match domain prefix specified organization_id='{}'",
                    oauthToken.getId(), organizationId);
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(oauthClient.getOrganizationId())
                            .withClientId(oauthClient.getId())
                            .withError(MSG_UNAUTHORIZED_REQUEST),
                    "Oauth2 token organization id='{}' does not match domain prefix specified organization id='{}'",
                    oauthToken.getId(), organizationId
            );
            throw new Oauth2Exception(MSG_UNAUTHORIZED_REQUEST);
        }

        if (defaultClaims.get(OAUTH2_ATTR_USER_ID) != null) {
            val userId = defaultClaims.get(OAUTH2_ATTR_USER_ID).toString();
            result.put(OAUTH2_ATTR_USER_ID, userId);
            val oauthUser = getUserById(userId, oauthClient);
            result.put(OAUTH2_ATTR_USERNAME, oauthUser.getUsername());
            try {
                result.put(OAUTH2_ATTR_METADATA, objectMapper.readValue(oauthUser.getMetadata(), Map.class));
            } catch (JsonProcessingException e) {
                result.put(OAUTH2_ATTR_METADATA, oauthUser.getMetadata());
            }
        }
        return result.build();
    }

    private OauthUser getUserById(final String userId, final OauthClient oauthClient) {
        val oauthUser = oauthUserDao.getById(userId);
        if (oauthUser.isEmpty()) {
            log.debug("OauthUser not found by id='{}'", userId);
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(oauthClient.getOrganizationId())
                            .withClientId(oauthClient.getId())
                            .withError(MSG_UNAUTHORIZED_REQUEST),
                    "Oauth2 user not found by id='%s'", userId
            );
            throw new Oauth2Exception(MSG_UNAUTHORIZED_REQUEST);
        }
        if (!oauthUser.get().isEnabled()) {
            log.debug("OauthUser user disabled. id='{}'", oauthUser.get().getId());
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(oauthClient.getOrganizationId())
                            .withClientId(oauthClient.getId())
                            .withError(MSG_UNAUTHORIZED_REQUEST),
                    "OauthUser user disabled. id='%s'", oauthUser.get().getId()
            );
            throw new Oauth2Exception(MSG_UNAUTHORIZED_REQUEST);
        }
        return oauthUser.get();
    }

    private Map<String, Object> getStandardAccessTokenDetails(final OauthToken oauthToken, final OauthClient oauthClient) {
        val result = ImmutableMap.<String, Object>builder();
        val now = Instant.now(defaultClock);
        if (now.isAfter(oauthToken.getExpiration())) {
            return result.put(OAUTH2_ATTR_ACTIVE, false).build();
        }
        result.put(OAUTH2_ATTR_ACTIVE, true);
        result.put(OAUTH2_ATTR_EXPIRES_IN, Duration.between(now, oauthToken.getExpiration()).toSeconds());
        result.put(OAUTH2_ATTR_EXPIRES, oauthToken.getExpiration().toEpochMilli() / 1000);
        if (isNotEmpty(oauthToken.getScopes())) {
            result.put(OAUTH2_ATTR_SCOPE, join(SPACE, oauthToken.getScopes()));
        }
        result.put(OAUTH2_ATTR_CLIENT_ID, oauthToken.getClientId());
        result.put(OAUTH2_ATTR_ORGANIZATION_ID, oauthToken.getOrganizationId());

        if (oauthToken.getOauthUserId() != null) {
            result.put(OAUTH2_ATTR_USER_ID, oauthToken.getOauthUserId());
            final OauthUser oauthUser = getUserById(oauthToken.getOauthUserId(), oauthClient);
            try {
                result.put(OAUTH2_ATTR_USERNAME, oauthUser.getUsername());
                result.put(OAUTH2_ATTR_METADATA, objectMapper.readValue(oauthUser.getMetadata(), Map.class));
            } catch (JsonProcessingException e) {
                result.put(OAUTH2_ATTR_METADATA, oauthUser.getMetadata());
            }
        }
        return result.build();
    }
}
