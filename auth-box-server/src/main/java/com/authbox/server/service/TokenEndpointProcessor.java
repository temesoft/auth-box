package com.authbox.server.service;

import com.authbox.base.dao.OauthTokenDao;
import com.authbox.base.dao.OauthUserDao;
import com.authbox.base.exception.BadRequestException;
import com.authbox.base.model.AccessLog;
import com.authbox.base.model.GrantType;
import com.authbox.base.model.OauthClient;
import com.authbox.base.model.OauthToken;
import com.authbox.base.model.OauthTokenResponse;
import com.authbox.base.model.OauthUser;
import com.authbox.base.model.Organization;
import com.authbox.base.service.AccessLogService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.PrivateKey;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.authbox.base.config.Constants.OAUTH2_ATTR_METADATA;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_ORGANIZATION_ID;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_SCOPE;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_USER_ID;
import static com.authbox.base.config.Constants.OAUTH2_TOKEN_TYPE_BEARER;
import static com.authbox.base.config.Constants.SPACE_SPLITTER;
import static com.authbox.base.model.GrantType.authorization_code;
import static com.authbox.base.model.GrantType.password;
import static com.authbox.base.model.TokenType.ACCESS_TOKEN;
import static com.authbox.base.model.TokenType.REFRESH_TOKEN;
import static com.authbox.base.util.CertificateKeysUtils.generatePrivateKey;
import static com.authbox.base.util.HashUtils.sha256;
import static com.authbox.server.util.RequestUtils.getRequestId;
import static com.authbox.server.util.RequestUtils.getTimeSinceRequest;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Slf4j
public abstract class TokenEndpointProcessor {

    @Autowired
    protected Clock defaultClock;
    @Autowired
    protected OauthTokenDao oauthTokenDao;
    @Autowired
    protected OauthUserDao oauthUserDao;
    @Autowired
    protected PasswordEncoder passwordEncoder;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected ParsingValidationService parsingValidationService;
    @Autowired
    protected AccessLogService accessLogService;

    public abstract OauthTokenResponse process(final Organization organization, final HttpServletRequest req, final HttpServletResponse res);

    public abstract GrantType getProcessingGrantType();

    protected OauthTokenResponse createJwtAccessToken(final Organization organization,
                                                      final OauthClient oauthClient,
                                                      final String scope,
                                                      final GrantType grantType,
                                                      @Nullable final OauthUser oauthUser,
                                                      final String ip,
                                                      final String userAgent,
                                                      @Nullable final String parentTokenId) {
        val now = Instant.now(defaultClock);
        val expiration = Instant.now(defaultClock).plusSeconds(oauthClient.getExpiration().toSeconds());
        val stopwatch = Stopwatch.createStarted();
        log.debug("Prepare keys for JWT token");
        accessLogService.create(
                AccessLog.builder()
                        .withRequestId(getRequestId())
                        .withDuration(getTimeSinceRequest())
                        .withOrganizationId(organization.getId())
                        .withClientId(oauthClient.getId()),
                "Preparing private key for JWT token creation"
        );
        final PrivateKey privateKey;
        try {
            privateKey = generatePrivateKey(oauthClient.getPrivateKey());
        } catch (IllegalArgumentException e) {
            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.getId())
                            .withClientId(oauthClient.getId())
                            .withError("bad request"),
                    "Error creating private key for JWT token signing: %s",
                    e.getMessage()
            );
            throw new BadRequestException(e.getMessage());
        }

        val refreshToken = createRefreshTokenIfNeeded(grantType, oauthClient, scope, oauthUser, ip, userAgent);
        log.debug("Sign JWT token");
        accessLogService.create(
                AccessLog.builder()
                        .withRequestId(getRequestId())
                        .withDuration(getTimeSinceRequest())
                        .withOrganizationId(organization.getId())
                        .withClientId(oauthClient.getId()),
                "Signing JWT access token using private key"
        );
        val jwsBuilder = Jwts.builder()
                .issuer(organization.getId())
                .subject(oauthClient.getId())
                .claim(OAUTH2_ATTR_SCOPE, scope)
                .claim(OAUTH2_ATTR_ORGANIZATION_ID, organization.getId())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(privateKey);
        if (oauthUser != null) {
            jwsBuilder.claim(OAUTH2_ATTR_USER_ID, oauthUser.getId());

            Object metadata = oauthUser.getMetadata();
            try {
                metadata = objectMapper.readValue(oauthUser.getMetadata(), Map.class);
            } catch (JsonProcessingException e) {
                log.debug("Unable to parse metadata for OauthUser user_id='{}'", oauthUser.getId());
            }

            jwsBuilder.claim(OAUTH2_ATTR_METADATA, metadata);
        }

        val jwt = jwsBuilder.compact();
        log.debug("JWT token created in: {}", stopwatch.stop());

        val oauthTokenId = UUID.randomUUID().toString();
        val oauthToken = new OauthToken(
                oauthTokenId,
                now,
                sha256(jwt),
                organization.getId(),
                oauthClient.getId(),
                expiration,
                SPACE_SPLITTER.splitToList(scope),
                oauthUser != null ? oauthUser.getId() : null,
                ACCESS_TOKEN,
                ip,
                userAgent,
                getRequestId(),
                null
        );

        accessLogService.create(
                AccessLog.builder()
                        .withRequestId(getRequestId())
                        .withDuration(getTimeSinceRequest())
                        .withOrganizationId(organization.getId())
                        .withClientId(oauthClient.getId())
                        .withOauthTokenId(oauthTokenId),
                "Inserting JWT access token into DB"
        );

        oauthTokenDao.insert(oauthToken);

        if (isNotEmpty(parentTokenId)) {
            oauthTokenDao.updateLinkedTokenId(parentTokenId, oauthToken.getId());
        }

        return new OauthTokenResponse(
                jwt,
                OAUTH2_TOKEN_TYPE_BEARER,
                oauthClient.getExpiration().toSeconds(),
                refreshToken.orElse(null),
                (isNotBlank(scope) ? scope : null)
        );
    }

    protected OauthTokenResponse createStandardAccessToken(final Organization organization,
                                                           final OauthClient oauthClient,
                                                           final String scope,
                                                           final GrantType grantType,
                                                           @Nullable final OauthUser oauthUser,
                                                           final String ip,
                                                           final String userAgent,
                                                           @Nullable final String parentTokenId) {
        val now = Instant.now(defaultClock);
        val expiration = now.plusSeconds(oauthClient.getExpiration().toSeconds());
        val refreshToken = createRefreshTokenIfNeeded(grantType, oauthClient, scope, oauthUser, ip, userAgent);
        val accessToken = sha256(UUID.randomUUID().toString());
        val oauthTokenId = UUID.randomUUID().toString();
        val oauthToken = new OauthToken(
                oauthTokenId,
                now,
                sha256(accessToken),
                organization.getId(),
                oauthClient.getId(),
                expiration,
                SPACE_SPLITTER.splitToList(scope),
                oauthUser != null ? oauthUser.getId() : null,
                ACCESS_TOKEN,
                ip,
                userAgent,
                getRequestId(),
                null
        );
        accessLogService.create(
                AccessLog.builder()
                        .withRequestId(getRequestId())
                        .withDuration(getTimeSinceRequest())
                        .withOauthTokenId(oauthTokenId)
                        .withOrganizationId(organization.getId())
                        .withClientId(oauthClient.getId()),
                "Inserting Oauth2 token object into DB"
        );
        oauthTokenDao.insert(oauthToken);

        if (isNotEmpty(parentTokenId)) {
            oauthTokenDao.updateLinkedTokenId(parentTokenId, oauthToken.getId());
        }

        return new OauthTokenResponse(
                accessToken,
                OAUTH2_TOKEN_TYPE_BEARER,
                oauthClient.getExpiration().toSeconds(),
                refreshToken.orElse(null),
                (isNotBlank(scope) ? scope : null)
        );
    }

    private Optional<String> createRefreshTokenIfNeeded(final GrantType grantType,
                                                        final OauthClient oauthClient,
                                                        final String scope,
                                                        @Nullable final OauthUser oauthUser,
                                                        final String ip,
                                                        final String userAgent) {
        if (isEmpty(oauthClient.getGrantTypes())
            || !oauthClient.getGrantTypes().contains(GrantType.refresh_token)) {
            return Optional.empty();
        }
        if (grantType == authorization_code || grantType == password) {
            val now = Instant.now(defaultClock);
            val expiration = Instant.now(defaultClock).plusSeconds(oauthClient.getRefreshExpiration().toSeconds());
            val token = sha256(UUID.randomUUID().toString());
            val refreshToken = new OauthToken(
                    UUID.randomUUID().toString(),
                    now,
                    sha256(token),
                    oauthClient.getOrganizationId(),
                    oauthClient.getId(),
                    expiration,
                    SPACE_SPLITTER.splitToList(scope),
                    oauthUser != null ? oauthUser.getId() : null,
                    REFRESH_TOKEN,
                    ip,
                    userAgent,
                    getRequestId(),
                    null
            );

            accessLogService.create(
                    AccessLog.builder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(oauthClient.getOrganizationId())
                            .withClientId(oauthClient.getId())
                            .withOauthTokenId(refreshToken.getId()),
                    "Inserting refresh token into DB"
            );

            oauthTokenDao.insert(refreshToken);
            return Optional.of(token);
        }
        return Optional.empty();
    }
}
