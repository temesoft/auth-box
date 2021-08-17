package com.authbox.server.service;

import com.authbox.base.dao.OauthTokenDao;
import com.authbox.base.dao.OauthUserDao;
import com.authbox.base.exception.BadRequestException;
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
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
import static com.authbox.base.model.AccessLog.AccessLogBuilder.accessLogBuilder;
import static com.authbox.base.model.GrantType.authorization_code;
import static com.authbox.base.model.GrantType.password;
import static com.authbox.base.model.TokenType.ACCESS_TOKEN;
import static com.authbox.base.model.TokenType.REFRESH_TOKEN;
import static com.authbox.base.util.CertificateKeysUtils.generatePrivateKey;
import static com.authbox.base.util.HashUtils.sha256;
import static com.authbox.server.util.RequestUtils.getRequestId;
import static com.authbox.server.util.RequestUtils.getTimeSinceRequest;
import static io.jsonwebtoken.SignatureAlgorithm.RS384;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

public abstract class TokenEndpointProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenEndpointProcessor.class);

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
        final Instant now = Instant.now(defaultClock);
        final Instant expiration = Instant.now(defaultClock).plusSeconds(oauthClient.expiration.toSeconds());
        final Stopwatch stopwatch = Stopwatch.createStarted();
        LOGGER.debug("Prepare keys for JWT token");
        accessLogService.create(
                accessLogBuilder()
                        .withRequestId(getRequestId())
                        .withDuration(getTimeSinceRequest())
                        .withOrganizationId(organization.id)
                        .withClientId(oauthClient.id),
                "Preparing private key for JWT token creation"
        );
        final PrivateKey privateKey;
        try {
            privateKey = generatePrivateKey(oauthClient.privateKey);
        } catch (IllegalArgumentException e) {
            accessLogService.create(
                    accessLogBuilder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(organization.id)
                            .withClientId(oauthClient.id)
                            .withError("bad request"),
                    "Error creating private key for JWT token signing: %s",
                    e.getMessage()
            );
            throw new BadRequestException(e.getMessage());
        }

        final Optional<String> refreshToken = createRefreshTokenIfNeeded(grantType, oauthClient, scope, oauthUser, ip, userAgent);
        LOGGER.debug("Sign JWT token");
        accessLogService.create(
                accessLogBuilder()
                        .withRequestId(getRequestId())
                        .withDuration(getTimeSinceRequest())
                        .withOrganizationId(organization.id)
                        .withClientId(oauthClient.id),
                "Signing JWT access token using private key"
        );
        final JwtBuilder jwsBuilder = Jwts.builder()
                .setIssuer(organization.id)
                .setSubject(oauthClient.id)
                .claim(OAUTH2_ATTR_SCOPE, scope)
                .claim(OAUTH2_ATTR_ORGANIZATION_ID, organization.id)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .signWith(privateKey, RS384);
        if (oauthUser != null) {
            jwsBuilder.claim(OAUTH2_ATTR_USER_ID, oauthUser.id);

            Object metadata = oauthUser.metadata;
            try {
                metadata = objectMapper.readValue(oauthUser.metadata, Map.class);
            } catch (JsonProcessingException e) {
                LOGGER.debug("Unable to parse metadata for OauthUser user_id='{}'", oauthUser.id);
            }

            jwsBuilder.claim(OAUTH2_ATTR_METADATA, metadata);
        }

        final String jwt = jwsBuilder.compact();
        LOGGER.debug("JWT token created in: " + stopwatch.stop());

        final String oauthTokenId = UUID.randomUUID().toString();
        final OauthToken oauthToken = new OauthToken(
                oauthTokenId,
                now,
                sha256(jwt),
                organization.id,
                oauthClient.id,
                expiration,
                SPACE_SPLITTER.splitToList(scope),
                oauthUser != null ? oauthUser.id : null,
                ACCESS_TOKEN,
                ip,
                userAgent,
                getRequestId()
        );

        accessLogService.create(
                accessLogBuilder()
                        .withRequestId(getRequestId())
                        .withDuration(getTimeSinceRequest())
                        .withOrganizationId(organization.id)
                        .withClientId(oauthClient.id)
                        .withOauthTokenId(oauthTokenId),
                "Inserting JWT access token into DB"
        );

        oauthTokenDao.insert(oauthToken);

        if (isNotEmpty(parentTokenId)) {
            oauthTokenDao.updateLinkedTokenId(parentTokenId, oauthToken.id);
        }

        return new OauthTokenResponse(
                jwt,
                OAUTH2_TOKEN_TYPE_BEARER,
                oauthClient.expiration.toSeconds(),
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
        final Instant now = Instant.now(defaultClock);
        final Instant expiration = now.plusSeconds(oauthClient.expiration.toSeconds());

        final Optional<String> refreshToken = createRefreshTokenIfNeeded(grantType, oauthClient, scope, oauthUser, ip, userAgent);

        final String accessToken = sha256(UUID.randomUUID().toString());
        final String oauthTokenId = UUID.randomUUID().toString();
        final OauthToken oauthToken = new OauthToken(
                oauthTokenId,
                now,
                sha256(accessToken),
                organization.id,
                oauthClient.id,
                expiration,
                SPACE_SPLITTER.splitToList(scope),
                oauthUser != null ? oauthUser.id : null,
                ACCESS_TOKEN,
                ip,
                userAgent,
                getRequestId()
        );
        accessLogService.create(
                accessLogBuilder()
                        .withRequestId(getRequestId())
                        .withDuration(getTimeSinceRequest())
                        .withOauthTokenId(oauthTokenId)
                        .withOrganizationId(organization.id)
                        .withClientId(oauthClient.id),
                "Inserting Oauth2 token object into DB"
        );
        oauthTokenDao.insert(oauthToken);

        if (isNotEmpty(parentTokenId)) {
            oauthTokenDao.updateLinkedTokenId(parentTokenId, oauthToken.id);
        }

        return new OauthTokenResponse(
                accessToken,
                OAUTH2_TOKEN_TYPE_BEARER,
                oauthClient.expiration.toSeconds(),
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
        if (grantType == authorization_code || grantType == password) {
            final Instant now = Instant.now(defaultClock);
            final Instant expiration = Instant.now(defaultClock).plusSeconds(oauthClient.refreshExpiration.toSeconds());
            final String token = sha256(UUID.randomUUID().toString());
            final OauthToken refreshToken = new OauthToken(
                    UUID.randomUUID().toString(),
                    now,
                    sha256(token),
                    oauthClient.organizationId,
                    oauthClient.id,
                    expiration,
                    SPACE_SPLITTER.splitToList(scope),
                    oauthUser != null ? oauthUser.id : null,
                    REFRESH_TOKEN,
                    ip,
                    userAgent,
                    getRequestId()
            );

            accessLogService.create(
                    accessLogBuilder()
                            .withRequestId(getRequestId())
                            .withDuration(getTimeSinceRequest())
                            .withOrganizationId(oauthClient.organizationId)
                            .withClientId(oauthClient.id)
                            .withOauthTokenId(refreshToken.id),
                    "Inserting refresh token into DB"
            );

            oauthTokenDao.insert(refreshToken);
            return Optional.of(token);
        }
        return Optional.empty();
    }
}
