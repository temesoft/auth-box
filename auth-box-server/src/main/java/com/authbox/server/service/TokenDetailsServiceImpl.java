package com.authbox.server.service;

import com.authbox.base.dao.OauthClientDao;
import com.authbox.base.dao.OauthTokenDao;
import com.authbox.base.dao.OauthUserDao;
import com.authbox.base.exception.BadRequestException;
import com.authbox.base.exception.Oauth2Exception;
import com.authbox.base.model.OauthClient;
import com.authbox.base.model.OauthToken;
import com.authbox.base.model.OauthUser;
import com.authbox.base.model.Organization;
import com.authbox.base.service.AccessLogService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.impl.DefaultClaims;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.security.PublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

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
import static com.authbox.base.model.AccessLog.AccessLogBuilder.accessLogBuilder;
import static com.authbox.base.model.TokenType.ACCESS_TOKEN;
import static com.authbox.base.util.CertificateKeysUtils.generatePublicKey;
import static com.authbox.base.util.HashUtils.sha256;
import static com.authbox.server.util.RequestUtils.getRequestId;
import static com.authbox.server.util.RequestUtils.getTimeSinceRequest;
import static java.lang.String.join;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.springframework.util.ObjectUtils.isEmpty;

public class TokenDetailsServiceImpl implements TokenDetailsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenDetailsServiceImpl.class);

    private final OauthTokenDao oauthTokenDao;
    private final OauthUserDao oauthUserDao;
    private final OauthClientDao oauthClientDao;
    private final Clock defaultClock;
    private final ObjectMapper objectMapper;
    private final AccessLogService accessLogService;


    public TokenDetailsServiceImpl(final OauthTokenDao oauthTokenDao, final OauthUserDao oauthUserDao, final OauthClientDao oauthClientDao, final Clock defaultClock, final ObjectMapper objectMapper, final AccessLogService accessLogService) {
        this.oauthTokenDao = oauthTokenDao;
        this.oauthUserDao = oauthUserDao;
        this.oauthClientDao = oauthClientDao;
        this.defaultClock = defaultClock;
        this.objectMapper = objectMapper;
        this.accessLogService = accessLogService;
    }

    @Override
    public Map<String, Object> getAccessTokenDetails(final Organization organization, final String accessToken, @Nullable final OauthClient providedClient) {
        accessLogService.create(
                accessLogBuilder()
                        .withRequestId(getRequestId())
                        .withDuration(getTimeSinceRequest())
                        .withOrganizationId(organization.id),
                "Validating Oauth2 access token details"
        );
        final String hash = sha256(accessToken);
        final Optional<OauthToken> oauthToken = oauthTokenDao.getByHash(hash);
        if (oauthToken.isEmpty()) {
            LOGGER.debug("Unable to find OauthToken by access_token='{}', hash='{}'", accessToken, hash);
            accessLogService.create(
                    accessLogBuilder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.id)
                            .withError(MSG_UNAUTHORIZED_REQUEST),
                    "Unable to find Oauth2 token by hash='%s'", hash
            );
            throw new Oauth2Exception(MSG_UNAUTHORIZED_REQUEST);
        }
        if (oauthToken.get().tokenType != ACCESS_TOKEN) {
            LOGGER.debug("OauthToken is not ACCESS_TOKEN type. access_token='{}', hash='{}'", accessToken, hash);
            accessLogService.create(
                    accessLogBuilder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.id)
                            .withOauthTokenId(oauthToken.get().id)
                            .withError(MSG_UNAUTHORIZED_REQUEST),
                    "Oauth2 token is not ACCESS_TOKEN type. hash='%s'", hash
            );
            throw new Oauth2Exception(MSG_UNAUTHORIZED_REQUEST);
        }
        if (!organization.id.equals(oauthToken.get().organizationId)) {
            LOGGER.debug("OauthToken organization_id='{}' does not match OauthClient specified organization_id='{}'",
                    oauthToken.get().organizationId, organization.id);
            accessLogService.create(
                    accessLogBuilder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.id)
                            .withError(MSG_UNAUTHORIZED_REQUEST),
                    "Oauth2 token organization id='%s' does not match Oauth2 client specified organization id='%s'",
                    oauthToken.get().organizationId, organization.id
            );
            throw new Oauth2Exception(MSG_UNAUTHORIZED_REQUEST);
        }

        accessLogService.create(
                accessLogBuilder()
                        .withRequestId(getRequestId())
                        .withDuration(getTimeSinceRequest())
                        .withOauthTokenId(oauthToken.get().id)
                        .withOrganizationId(organization.id),
                "Oauth2 token validated"
        );

        final Optional<OauthClient> oauthClient = oauthClientDao.getById(oauthToken.get().clientId);
        if (oauthClient.isEmpty()) {
            LOGGER.debug("Unable to find OauthClient by client_id='{}'", oauthToken.get().clientId);
            accessLogService.create(
                    accessLogBuilder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.id)
                            .withError(MSG_UNAUTHORIZED_REQUEST),
                    "Unable to find Oauth2 client by client id='%s'", oauthToken.get().clientId
            );
            throw new Oauth2Exception(MSG_UNAUTHORIZED_REQUEST);
        }
        if (!oauthClient.get().enabled) {
            LOGGER.debug("OauthClient is disabled. client_id='{}'", oauthToken.get().clientId);
            accessLogService.create(
                    accessLogBuilder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.id)
                            .withClientId(oauthClient.get().id)
                            .withError(MSG_UNAUTHORIZED_REQUEST),
                    "Oauth2 client is disabled. client id='%s'", oauthToken.get().clientId
            );
            throw new Oauth2Exception(MSG_UNAUTHORIZED_REQUEST);
        }

        if (providedClient != null && !providedClient.id.equals(oauthToken.get().clientId)) {
            LOGGER.debug("OauthClient provided (client_id: {}) does not correspond to OauthClient associated with provided token (client_id: {})", providedClient.id, oauthToken.get().clientId);
            accessLogService.create(
                    accessLogBuilder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.id)
                            .withClientId(oauthClient.get().id)
                            .withError(MSG_UNAUTHORIZED_REQUEST),
                    "Oauth2 client provided (client id: %s) does not correspond to Oauth2 client associated with provided token (client id: %s)", providedClient.id, oauthToken.get().clientId
            );
            throw new Oauth2Exception(MSG_UNAUTHORIZED_REQUEST);
        }

        if (!organization.id.equals(oauthClient.get().organizationId)) {
            LOGGER.debug("OauthClient organization_id='{}' does not match OauthClient specified organization_id='{}'",
                    oauthToken.get().organizationId, organization.id);
            accessLogService.create(
                    accessLogBuilder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.id)
                            .withClientId(oauthClient.get().id)
                            .withError(MSG_UNAUTHORIZED_REQUEST),
                    "Oauth2 client organization id='%s' does not match Oauth2 client specified organization id='%s'",
                    oauthToken.get().organizationId, organization.id
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
        if (isEmpty(oauthClient.publicKey)) {
            LOGGER.debug("Client with oauth_client_id='{}' does not have public key to validate JWT token.", oauthClient.id);
            accessLogService.create(
                    accessLogBuilder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(oauthClient.organizationId)
                            .withClientId(oauthClient.id)
                            .withOauthTokenId(oauthToken.id)
                            .withError(MSG_INVALID_REQUEST),
                    "Client with oauth_client_id='{}' does not have public key to validate JWT token.", oauthClient.id
            );
            throw new BadRequestException(MSG_INVALID_REQUEST);
        }

        final PublicKey publicKey;
        try {
            publicKey = generatePublicKey(oauthClient.publicKey);
        } catch (IllegalArgumentException e) {
            accessLogService.create(
                    accessLogBuilder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(oauthClient.organizationId)
                            .withClientId(oauthClient.id)
                            .withError(MSG_UNAUTHORIZED_REQUEST),
                    e.getMessage()
            );
            throw new BadRequestException(e.getMessage());
        }

        final ImmutableMap.Builder<String, Object> result = ImmutableMap.builder();
        final Jwt jwt;
        try {
            jwt = Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parse(accessToken);
        } catch (MalformedJwtException e) {
            LOGGER.debug("Unable to parse JWT token: {}", e.getMessage());
            accessLogService.create(
                    accessLogBuilder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(oauthClient.organizationId)
                            .withClientId(oauthClient.id)
                            .withOauthTokenId(oauthToken.id)
                            .withError(MSG_UNAUTHORIZED_REQUEST),
                    "Unable to parse JWT token: %s", e.getMessage()
            );
            throw new Oauth2Exception(MSG_UNAUTHORIZED_REQUEST);
        } catch (ExpiredJwtException e) {
            accessLogService.create(
                    accessLogBuilder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(oauthClient.organizationId)
                            .withClientId(oauthClient.id)
                            .withOauthTokenId(oauthToken.id)
                            .withError(MSG_UNAUTHORIZED_REQUEST),
                    "Oauth2 JWT token is expired"
            );
            return result.put(OAUTH2_ATTR_ACTIVE, false).build();
        } catch (SignatureException e) {
            LOGGER.debug("Unable to verify JWT signature: {}", e.getMessage());
            accessLogService.create(
                    accessLogBuilder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(oauthClient.organizationId)
                            .withClientId(oauthClient.id)
                            .withOauthTokenId(oauthToken.id)
                            .withError(MSG_UNAUTHORIZED_REQUEST),
                    "Unable to verify JWT signature: %s", e.getMessage()
            );
            throw new Oauth2Exception(MSG_UNAUTHORIZED_REQUEST);
        }

        accessLogService.create(
                accessLogBuilder()
                        .withRequestId(getRequestId())
                        .withDuration(getTimeSinceRequest())
                        .withOrganizationId(oauthClient.organizationId)
                        .withClientId(oauthClient.id)
                        .withOauthTokenId(oauthToken.id),
                "Successfully validated JWT token and signature"
        );

        final DefaultClaims defaultClaims = (DefaultClaims) jwt.getBody();
        final Instant now = Instant.now(defaultClock);

        result.put(OAUTH2_ATTR_ACTIVE, true);
        result.put(OAUTH2_ATTR_EXPIRES_IN, Duration.between(now, defaultClaims.getExpiration().toInstant()).toSeconds());
        result.put(OAUTH2_ATTR_EXPIRES, defaultClaims.getExpiration().getTime() / 1000);
        result.put(OAUTH2_ATTR_SCOPE, defaultClaims.get(OAUTH2_ATTR_SCOPE));
        final String organizationId = (String) defaultClaims.get(OAUTH2_ATTR_ORGANIZATION_ID);
        result.put(OAUTH2_ATTR_ORGANIZATION_ID, organizationId);

        if (!oauthToken.organizationId.equals(organizationId)) {
            LOGGER.debug("OauthToken organization_id='{}' does not match domain prefix specified organization_id='{}'", oauthToken.id, organizationId);
            accessLogService.create(
                    accessLogBuilder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(oauthClient.organizationId)
                            .withClientId(oauthClient.id)
                            .withError(MSG_UNAUTHORIZED_REQUEST),
                    "Oauth2 token organization id='{}' does not match domain prefix specified organization id='{}'", oauthToken.id, organizationId
            );
            throw new Oauth2Exception(MSG_UNAUTHORIZED_REQUEST);
        }

        if (defaultClaims.get(OAUTH2_ATTR_USER_ID) != null) {
            final String userId = defaultClaims.get(OAUTH2_ATTR_USER_ID).toString();
            result.put(OAUTH2_ATTR_USER_ID, userId);
            final OauthUser oauthUser = getUserById(userId, oauthClient);
            result.put(OAUTH2_ATTR_USERNAME, oauthUser.username);
            try {
                result.put(OAUTH2_ATTR_METADATA, objectMapper.readValue(oauthUser.metadata, Map.class));
            } catch (JsonProcessingException e) {
                result.put(OAUTH2_ATTR_METADATA, oauthUser.metadata);
            }
        }
        return result.build();
    }

    private OauthUser getUserById(final String userId, final OauthClient oauthClient) {
        final Optional<OauthUser> oauthUser = oauthUserDao.getById(userId);
        if (oauthUser.isEmpty()) {
            LOGGER.debug("OauthUser not found by id='{}'", userId);
            accessLogService.create(
                    accessLogBuilder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(oauthClient.organizationId)
                            .withClientId(oauthClient.id)
                            .withError(MSG_UNAUTHORIZED_REQUEST),
                    "Oauth2 user not found by id='%s'", userId
            );
            throw new Oauth2Exception(MSG_UNAUTHORIZED_REQUEST);
        }
        if (!oauthUser.get().enabled) {
            LOGGER.debug("OauthUser user disabled. id='{}'", oauthUser.get().id);
            accessLogService.create(
                    accessLogBuilder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(oauthClient.organizationId)
                            .withClientId(oauthClient.id)
                            .withError(MSG_UNAUTHORIZED_REQUEST),
                    "OauthUser user disabled. id='%s'", oauthUser.get().id
            );
            throw new Oauth2Exception(MSG_UNAUTHORIZED_REQUEST);
        }
        return oauthUser.get();
    }

    private Map<String, Object> getStandardAccessTokenDetails(final OauthToken oauthToken, final OauthClient oauthClient) {
        final ImmutableMap.Builder<String, Object> result = ImmutableMap.builder();
        final Instant now = Instant.now(defaultClock);
        if (now.isAfter(oauthToken.expiration)) {
            return result.put(OAUTH2_ATTR_ACTIVE, false).build();
        }
        result.put(OAUTH2_ATTR_ACTIVE, true);
        result.put(OAUTH2_ATTR_EXPIRES_IN, Duration.between(now, oauthToken.expiration).toSeconds());
        result.put(OAUTH2_ATTR_EXPIRES, oauthToken.expiration.toEpochMilli() / 1000);
        if (isNotEmpty(oauthToken.scopes)) {
            result.put(OAUTH2_ATTR_SCOPE, join(SPACE, oauthToken.scopes));
        }
        result.put(OAUTH2_ATTR_CLIENT_ID, oauthToken.clientId);
        result.put(OAUTH2_ATTR_ORGANIZATION_ID, oauthToken.organizationId);

        if (oauthToken.oauthUserId != null) {
            result.put(OAUTH2_ATTR_USER_ID, oauthToken.oauthUserId);
            final OauthUser oauthUser = getUserById(oauthToken.oauthUserId, oauthClient);
            try {
                result.put(OAUTH2_ATTR_USERNAME, oauthUser.username);
                result.put(OAUTH2_ATTR_METADATA, objectMapper.readValue(oauthUser.metadata, Map.class));
            } catch (JsonProcessingException e) {
                result.put(OAUTH2_ATTR_METADATA, oauthUser.metadata);
            }
        }
        return result.build();
    }
}
