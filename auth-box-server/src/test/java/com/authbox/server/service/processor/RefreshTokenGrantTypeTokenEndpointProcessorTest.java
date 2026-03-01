package com.authbox.server.service.processor;

import com.authbox.base.dao.OauthClientDao;
import com.authbox.base.dao.OauthTokenDao;
import com.authbox.base.dao.OauthUserDao;
import com.authbox.base.exception.BadRequestException;
import com.authbox.base.exception.Oauth2Exception;
import com.authbox.base.model.OauthClient;
import com.authbox.base.model.OauthToken;
import com.authbox.base.model.OauthUser;
import com.authbox.base.model.Organization;
import com.authbox.base.model.TokenFormat;
import com.authbox.base.model.TokenType;
import com.authbox.base.service.AccessLogService;
import com.authbox.base.util.CertificateKeysUtils;
import com.authbox.server.service.ParsingValidationService;
import com.authbox.server.service.ParsingValidationServiceImpl;
import com.authbox.server.service.TokenEndpointProcessor;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.authbox.base.config.Constants.OAUTH2_ATTR_CLIENT_ID;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_CLIENT_SECRET;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_REFRESH_TOKEN;
import static com.authbox.base.util.CertificateKeysUtils.generatePublicKey;
import static com.authbox.base.util.HashUtils.sha256;
import static com.authbox.base.util.IdUtils.createId;
import static com.authbox.server.TestUtils.assertLogEntryContainsAndReset;
import static com.authbox.server.filter.RequestWrapperFilter.REQUEST_START_REQUEST_TIME_MDC_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RefreshTokenGrantTypeTokenEndpointProcessorTest {

    private static final String SCOPE = "some/scope";
    private static final String OAUTH_CLIENT_ID = createId();
    private static final String OAUTH_CLIENT_SECRET = createId();

    private RefreshTokenGrantTypeTokenEndpointProcessor processor;
    private OauthClientDao oauthClientDao;
    private OauthUserDao oauthUserDao;
    private AccessLogService accessLogService;
    private OauthTokenDao oauthTokenDao;
    private HttpServletRequest req;
    private HttpServletResponse res;

    @BeforeEach
    public void setup() {
        oauthClientDao = mock(OauthClientDao.class);
        oauthUserDao = mock(OauthUserDao.class);
        oauthTokenDao = mock(OauthTokenDao.class);
        val passwordEncoder = new BCryptPasswordEncoder();
        val defaultClock = Clock.systemUTC();
        processor = new RefreshTokenGrantTypeTokenEndpointProcessor();
        accessLogService = mock(AccessLogService.class);
        val objectMapper = new ObjectMapper();
        val parsingValidationService = new ParsingValidationServiceImpl(oauthClientDao, accessLogService);
        ReflectionTestUtils.setField(processor, TokenEndpointProcessor.class, "defaultClock", defaultClock, Clock.class);
        ReflectionTestUtils.setField(processor, TokenEndpointProcessor.class, "oauthTokenDao", oauthTokenDao, OauthTokenDao.class);
        ReflectionTestUtils.setField(processor, TokenEndpointProcessor.class, "oauthUserDao", oauthUserDao, OauthUserDao.class);
        ReflectionTestUtils.setField(processor, TokenEndpointProcessor.class, "passwordEncoder", passwordEncoder, PasswordEncoder.class);
        ReflectionTestUtils.setField(processor, TokenEndpointProcessor.class, "objectMapper", objectMapper, ObjectMapper.class);
        ReflectionTestUtils.setField(processor, TokenEndpointProcessor.class, "parsingValidationService", parsingValidationService, ParsingValidationService.class);
        ReflectionTestUtils.setField(processor, TokenEndpointProcessor.class, "accessLogService", accessLogService, AccessLogService.class);
        req = mock(HttpServletRequest.class);
        res = mock(HttpServletResponse.class);
        MDC.put(REQUEST_START_REQUEST_TIME_MDC_KEY, System.currentTimeMillis() + "");
    }

    @AfterEach
    public void teardown() {
        MDC.clear();
    }

    @Test
    public void testProcess_OpaqueToken_WithUser() {
        val organization = Organization.builder()
                .withId(createId())
                .withCreateTime(Instant.now())
                .withDomainPrefix("some.domain")
                .withEnabled(false)
                .withName("Some Organization")
                .build();

        assertThatThrownBy(() -> processor.process(organization, req, res))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Parsing and validating Oauth2 client",
                "Request missing client credentials"
        );

        when(req.getParameter(OAUTH2_ATTR_CLIENT_ID)).thenReturn(OAUTH_CLIENT_ID);
        when(req.getParameter(OAUTH2_ATTR_CLIENT_SECRET)).thenReturn(OAUTH_CLIENT_SECRET);
        assertThatThrownBy(() -> processor.process(organization, req, res))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Parsing and validating Oauth2 client",
                "Oauth2 client not found by id='%s'".formatted(OAUTH_CLIENT_ID)
        );

        val oauthClient = OauthClient.builder()
                .withId(OAUTH_CLIENT_ID)
                .withCreateTime(Instant.now())
                .withSecret(OAUTH_CLIENT_SECRET)
                .withEnabled(false)
                .withOrganizationId("bad-org-id")
                .withRedirectUrls(List.of("http://bad-redirect-url"))
                .withScopes(List.of())
                .withTokenFormat(TokenFormat.STANDARD)
                .withExpiration(Duration.ofMinutes(10))
                .build();
        when(oauthClientDao.getById(OAUTH_CLIENT_ID)).thenReturn(Optional.of(oauthClient));
        assertThatThrownBy(() -> processor.process(organization, req, res))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Parsing and validating Oauth2 client",
                "Oauth2 client is disabled"
        );

        oauthClient.setEnabled(true);
        assertThatThrownBy(() -> processor.process(organization, req, res))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Parsing and validating Oauth2 client",
                "Oauth2 client organization details do not match domain prefix specified in request: 'some.domain'"
        );

        oauthClient.setOrganizationId(organization.getId());
        assertThatThrownBy(() -> processor.process(organization, req, res))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Parsing and validating Oauth2 client",
                "Refresh token is not provided or empty"
        );

        val refreshToken = createId();
        val hash = sha256(refreshToken);
        when(req.getParameter(OAUTH2_ATTR_REFRESH_TOKEN)).thenReturn(refreshToken);
        assertThatThrownBy(() -> processor.process(organization, req, res))
                .isInstanceOf(Oauth2Exception.class)
                .hasMessageContaining("invalid token");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Parsing and validating Oauth2 client",
                "Refresh token hash='%s' not found".formatted(hash)
        );

        val oauthUser = OauthUser.builder()
                .withId(createId())
                .withCreateTime(Instant.now())
                .withUsername("username")
                .withOrganizationId(organization.getId())
                .withPassword("password")
                .withMetadata("{\"test\":123}")
                .withSecret(createId())
                .withLastUpdated(Instant.now())
                .build();

        val oauthToken = OauthToken.builder()
                .withId(createId())
                .withCreateTime(Instant.now())
                .withScopes(List.of(SCOPE))
                .withHash(hash)
                .withClientId(oauthClient.getId())
                .withOauthUserId(oauthUser.getId())
                .withOrganizationId(organization.getId())
                .withTokenType(TokenType.ACCESS_TOKEN)
                .withLinkedTokenId("already-used")
                .withExpiration(Instant.now().minusSeconds(10))
                .build();
        when(oauthTokenDao.getByHash(hash)).thenReturn(Optional.of(oauthToken));
        assertThatThrownBy(() -> processor.process(organization, req, res))
                .isInstanceOf(Oauth2Exception.class)
                .hasMessageContaining("invalid token");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Parsing and validating Oauth2 client",
                "Provided token is not REFRESH_TOKEN. type='ACCESS_TOKEN' hash='%s'".formatted(hash)
        );

        oauthToken.setTokenType(TokenType.REFRESH_TOKEN);
        assertThatThrownBy(() -> processor.process(organization, req, res))
                .isInstanceOf(Oauth2Exception.class)
                .hasMessageContaining("invalid token");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Parsing and validating Oauth2 client",
                "Refresh token expired hash='%s'".formatted(hash)
        );

        oauthToken.setExpiration(Instant.now().plusSeconds(10));
        assertThatThrownBy(() -> processor.process(organization, req, res))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Parsing and validating Oauth2 client",
                "Oauth2 user not found by id='%s'".formatted(oauthUser.getId())
        );

        when(oauthUserDao.getById(oauthUser.getId())).thenReturn(Optional.of(oauthUser));
        val oauthTokenResponse = processor.process(organization, req, res);
        assertLogEntryContainsAndReset(
                accessLogService,
                "Parsing and validating Oauth2 client",
                "Inserting Oauth2 token object into DB"
        );
        assertThat(oauthTokenResponse).isNotNull();
        assertThat(oauthTokenResponse.tokenType).isEqualTo("bearer");
        assertThat(oauthTokenResponse.expiresIn).isEqualTo(600);
        assertThat(oauthTokenResponse.refreshToken).isNull();
        assertThat(oauthTokenResponse.scope).isEqualTo(SCOPE);
        assertThat(oauthTokenResponse.accessToken).hasSize(64);
    }

    @Test
    public void testProcess_OpaqueToken() {
        val organization = Organization.builder()
                .withId(createId())
                .withCreateTime(Instant.now())
                .withDomainPrefix("some.domain")
                .withEnabled(false)
                .withName("Some Organization")
                .build();

        assertThatThrownBy(() -> processor.process(organization, req, res))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Parsing and validating Oauth2 client",
                "Request missing client credentials"
        );

        when(req.getParameter(OAUTH2_ATTR_CLIENT_ID)).thenReturn(OAUTH_CLIENT_ID);
        when(req.getParameter(OAUTH2_ATTR_CLIENT_SECRET)).thenReturn(OAUTH_CLIENT_SECRET);
        assertThatThrownBy(() -> processor.process(organization, req, res))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Parsing and validating Oauth2 client",
                "Oauth2 client not found by id='%s'".formatted(OAUTH_CLIENT_ID)
        );

        val oauthClient = OauthClient.builder()
                .withId(OAUTH_CLIENT_ID)
                .withCreateTime(Instant.now())
                .withSecret(OAUTH_CLIENT_SECRET)
                .withEnabled(false)
                .withOrganizationId("bad-org-id")
                .withRedirectUrls(List.of("http://bad-redirect-url"))
                .withScopes(List.of())
                .withTokenFormat(TokenFormat.STANDARD)
                .withExpiration(Duration.ofMinutes(10))
                .build();
        when(oauthClientDao.getById(OAUTH_CLIENT_ID)).thenReturn(Optional.of(oauthClient));
        assertThatThrownBy(() -> processor.process(organization, req, res))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Parsing and validating Oauth2 client",
                "Oauth2 client is disabled"
        );

        oauthClient.setEnabled(true);
        assertThatThrownBy(() -> processor.process(organization, req, res))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Parsing and validating Oauth2 client",
                "Oauth2 client organization details do not match domain prefix specified in request: 'some.domain'"
        );

        oauthClient.setOrganizationId(organization.getId());
        assertThatThrownBy(() -> processor.process(organization, req, res))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Parsing and validating Oauth2 client",
                "Refresh token is not provided or empty"
        );

        val refreshToken = createId();
        val hash = sha256(refreshToken);
        when(req.getParameter(OAUTH2_ATTR_REFRESH_TOKEN)).thenReturn(refreshToken);
        assertThatThrownBy(() -> processor.process(organization, req, res))
                .isInstanceOf(Oauth2Exception.class)
                .hasMessageContaining("invalid token");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Parsing and validating Oauth2 client",
                "Refresh token hash='%s' not found".formatted(hash)
        );

        val oauthToken = OauthToken.builder()
                .withId(createId())
                .withCreateTime(Instant.now())
                .withScopes(List.of(SCOPE))
                .withHash(hash)
                .withClientId(oauthClient.getId())
                .withOauthUserId(null)
                .withOrganizationId(organization.getId())
                .withTokenType(TokenType.ACCESS_TOKEN)
                .withLinkedTokenId("already-used")
                .withExpiration(Instant.now().minusSeconds(10))
                .build();
        when(oauthTokenDao.getByHash(hash)).thenReturn(Optional.of(oauthToken));
        assertThatThrownBy(() -> processor.process(organization, req, res))
                .isInstanceOf(Oauth2Exception.class)
                .hasMessageContaining("invalid token");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Parsing and validating Oauth2 client",
                "Provided token is not REFRESH_TOKEN. type='ACCESS_TOKEN' hash='%s'".formatted(hash)
        );

        oauthToken.setTokenType(TokenType.REFRESH_TOKEN);
        assertThatThrownBy(() -> processor.process(organization, req, res))
                .isInstanceOf(Oauth2Exception.class)
                .hasMessageContaining("invalid token");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Parsing and validating Oauth2 client",
                "Refresh token expired hash='%s'".formatted(hash)
        );

        oauthToken.setExpiration(Instant.now().plusSeconds(10));
        val oauthTokenResponse = processor.process(organization, req, res);
        assertLogEntryContainsAndReset(
                accessLogService,
                "Parsing and validating Oauth2 client",
                "Inserting Oauth2 token object into DB"
        );
        assertThat(oauthTokenResponse).isNotNull();
        assertThat(oauthTokenResponse.tokenType).isEqualTo("bearer");
        assertThat(oauthTokenResponse.expiresIn).isEqualTo(600);
        assertThat(oauthTokenResponse.refreshToken).isNull();
        assertThat(oauthTokenResponse.scope).isEqualTo(SCOPE);
        assertThat(oauthTokenResponse.accessToken).hasSize(64);
    }

    @Test
    public void testProcess_JwtToken_WithUser() {
        val organization = Organization.builder()
                .withId(createId())
                .withCreateTime(Instant.now())
                .withDomainPrefix("some.domain")
                .withEnabled(false)
                .withName("Some Organization")
                .build();

        assertThatThrownBy(() -> processor.process(organization, req, res))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Parsing and validating Oauth2 client",
                "Request missing client credentials"
        );

        when(req.getParameter(OAUTH2_ATTR_CLIENT_ID)).thenReturn(OAUTH_CLIENT_ID);
        when(req.getParameter(OAUTH2_ATTR_CLIENT_SECRET)).thenReturn(OAUTH_CLIENT_SECRET);
        assertThatThrownBy(() -> processor.process(organization, req, res))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Parsing and validating Oauth2 client",
                "Oauth2 client not found by id='%s'".formatted(OAUTH_CLIENT_ID)
        );

        val rsaKeyPair = CertificateKeysUtils.generateRsaKeyPair();
        val oauthClient = OauthClient.builder()
                .withId(OAUTH_CLIENT_ID)
                .withCreateTime(Instant.now())
                .withSecret(OAUTH_CLIENT_SECRET)
                .withEnabled(false)
                .withOrganizationId("bad-org-id")
                .withRedirectUrls(List.of("http://bad-redirect-url"))
                .withScopes(List.of())
                .withPrivateKey(rsaKeyPair.privateKeyPem)
                .withPublicKey(rsaKeyPair.publicKeyPem)
                .withTokenFormat(TokenFormat.JWT)
                .withExpiration(Duration.ofMinutes(10))
                .build();
        when(oauthClientDao.getById(OAUTH_CLIENT_ID)).thenReturn(Optional.of(oauthClient));
        assertThatThrownBy(() -> processor.process(organization, req, res))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Parsing and validating Oauth2 client",
                "Oauth2 client is disabled"
        );

        oauthClient.setEnabled(true);
        assertThatThrownBy(() -> processor.process(organization, req, res))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Parsing and validating Oauth2 client",
                "Oauth2 client organization details do not match domain prefix specified in request: 'some.domain'"
        );

        oauthClient.setOrganizationId(organization.getId());
        assertThatThrownBy(() -> processor.process(organization, req, res))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Parsing and validating Oauth2 client",
                "Refresh token is not provided or empty"
        );

        val refreshToken = createId();
        val hash = sha256(refreshToken);
        when(req.getParameter(OAUTH2_ATTR_REFRESH_TOKEN)).thenReturn(refreshToken);
        assertThatThrownBy(() -> processor.process(organization, req, res))
                .isInstanceOf(Oauth2Exception.class)
                .hasMessageContaining("invalid token");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Parsing and validating Oauth2 client",
                "Refresh token hash='%s' not found".formatted(hash)
        );

        val oauthUser = OauthUser.builder()
                .withId(createId())
                .withCreateTime(Instant.now())
                .withUsername("username")
                .withOrganizationId(organization.getId())
                .withPassword("password")
                .withMetadata("{\"test\":123}")
                .withSecret(createId())
                .withLastUpdated(Instant.now())
                .build();
        val oauthToken = OauthToken.builder()
                .withId(createId())
                .withCreateTime(Instant.now())
                .withScopes(List.of(SCOPE))
                .withHash(hash)
                .withClientId(oauthClient.getId())
                .withOauthUserId(oauthUser.getId())
                .withOrganizationId(organization.getId())
                .withTokenType(TokenType.ACCESS_TOKEN)
                .withLinkedTokenId("already-used")
                .withExpiration(Instant.now().minusSeconds(10))
                .build();
        when(oauthTokenDao.getByHash(hash)).thenReturn(Optional.of(oauthToken));
        assertThatThrownBy(() -> processor.process(organization, req, res))
                .isInstanceOf(Oauth2Exception.class)
                .hasMessageContaining("invalid token");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Parsing and validating Oauth2 client",
                "Provided token is not REFRESH_TOKEN. type='ACCESS_TOKEN' hash='%s'".formatted(hash)
        );

        oauthToken.setTokenType(TokenType.REFRESH_TOKEN);
        assertThatThrownBy(() -> processor.process(organization, req, res))
                .isInstanceOf(Oauth2Exception.class)
                .hasMessageContaining("invalid token");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Parsing and validating Oauth2 client",
                "Refresh token expired hash='%s'".formatted(hash)
        );

        oauthToken.setExpiration(Instant.now().plusSeconds(10));
        assertThatThrownBy(() -> processor.process(organization, req, res))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Parsing and validating Oauth2 client",
                "Oauth2 user not found by id='%s'".formatted(oauthUser.getId())
        );

        when(oauthUserDao.getById(oauthUser.getId())).thenReturn(Optional.of(oauthUser));
        val oauthTokenResponse = processor.process(organization, req, res);
        assertLogEntryContainsAndReset(
                accessLogService,
                "Parsing and validating Oauth2 client",
                "Preparing private key for JWT token creation",
                "Signing JWT access token using private key",
                "Inserting JWT access token into DB"
        );
        assertThat(oauthTokenResponse).isNotNull();
        assertThat(oauthTokenResponse.tokenType).isEqualTo("bearer");
        assertThat(oauthTokenResponse.expiresIn).isEqualTo(600);
        assertThat(oauthTokenResponse.refreshToken).isNull();
        assertThat(oauthTokenResponse.scope).isEqualTo(SCOPE);
        assertThat(oauthTokenResponse.accessToken).hasSizeGreaterThan(64);

        val jwt = Jwts.parser()
                .verifyWith(generatePublicKey(oauthClient.getPublicKey()))
                .build()
                .parseSignedClaims(oauthTokenResponse.accessToken);
        assertThat(jwt).isNotNull();
        assertThat(jwt.getHeader()).isNotNull();
        assertThat(jwt.getHeader().get("alg")).isEqualTo("RS384");
        assertThat(jwt.getPayload()).isNotNull();
        assertThat(jwt.getPayload().get("iss")).isEqualTo(organization.getId());
        assertThat(jwt.getPayload().get("sub")).isEqualTo(oauthClient.getId());
        assertThat(jwt.getPayload().get("organization_id")).isEqualTo(organization.getId());
        assertThat(jwt.getPayload().get("iat")).isNotNull();
        assertThat(jwt.getPayload().get("exp")).isNotNull();
        assertThat(jwt.getDigest()).isNotNull();
    }

    @Test
    public void testProcess_JwtToken() {
        val organization = Organization.builder()
                .withId(createId())
                .withCreateTime(Instant.now())
                .withDomainPrefix("some.domain")
                .withEnabled(false)
                .withName("Some Organization")
                .build();

        assertThatThrownBy(() -> processor.process(organization, req, res))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Parsing and validating Oauth2 client",
                "Request missing client credentials"
        );

        when(req.getParameter(OAUTH2_ATTR_CLIENT_ID)).thenReturn(OAUTH_CLIENT_ID);
        when(req.getParameter(OAUTH2_ATTR_CLIENT_SECRET)).thenReturn(OAUTH_CLIENT_SECRET);
        assertThatThrownBy(() -> processor.process(organization, req, res))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Parsing and validating Oauth2 client",
                "Oauth2 client not found by id='%s'".formatted(OAUTH_CLIENT_ID)
        );

        val rsaKeyPair = CertificateKeysUtils.generateRsaKeyPair();
        val oauthClient = OauthClient.builder()
                .withId(OAUTH_CLIENT_ID)
                .withCreateTime(Instant.now())
                .withSecret(OAUTH_CLIENT_SECRET)
                .withEnabled(false)
                .withOrganizationId("bad-org-id")
                .withRedirectUrls(List.of("http://bad-redirect-url"))
                .withScopes(List.of())
                .withPrivateKey(rsaKeyPair.privateKeyPem)
                .withPublicKey(rsaKeyPair.publicKeyPem)
                .withTokenFormat(TokenFormat.JWT)
                .withExpiration(Duration.ofMinutes(10))
                .build();
        when(oauthClientDao.getById(OAUTH_CLIENT_ID)).thenReturn(Optional.of(oauthClient));
        assertThatThrownBy(() -> processor.process(organization, req, res))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Parsing and validating Oauth2 client",
                "Oauth2 client is disabled"
        );

        oauthClient.setEnabled(true);
        assertThatThrownBy(() -> processor.process(organization, req, res))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Parsing and validating Oauth2 client",
                "Oauth2 client organization details do not match domain prefix specified in request: 'some.domain'"
        );

        oauthClient.setOrganizationId(organization.getId());
        assertThatThrownBy(() -> processor.process(organization, req, res))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Parsing and validating Oauth2 client",
                "Refresh token is not provided or empty"
        );

        val refreshToken = createId();
        val hash = sha256(refreshToken);
        when(req.getParameter(OAUTH2_ATTR_REFRESH_TOKEN)).thenReturn(refreshToken);
        assertThatThrownBy(() -> processor.process(organization, req, res))
                .isInstanceOf(Oauth2Exception.class)
                .hasMessageContaining("invalid token");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Parsing and validating Oauth2 client",
                "Refresh token hash='%s' not found".formatted(hash)
        );

        val oauthToken = OauthToken.builder()
                .withId(createId())
                .withCreateTime(Instant.now())
                .withScopes(List.of(SCOPE))
                .withHash(hash)
                .withClientId(oauthClient.getId())
                .withOauthUserId(null)
                .withOrganizationId(organization.getId())
                .withTokenType(TokenType.ACCESS_TOKEN)
                .withLinkedTokenId("already-used")
                .withExpiration(Instant.now().minusSeconds(10))
                .build();
        when(oauthTokenDao.getByHash(hash)).thenReturn(Optional.of(oauthToken));
        assertThatThrownBy(() -> processor.process(organization, req, res))
                .isInstanceOf(Oauth2Exception.class)
                .hasMessageContaining("invalid token");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Parsing and validating Oauth2 client",
                "Provided token is not REFRESH_TOKEN. type='ACCESS_TOKEN' hash='%s'".formatted(hash)
        );

        oauthToken.setTokenType(TokenType.REFRESH_TOKEN);
        assertThatThrownBy(() -> processor.process(organization, req, res))
                .isInstanceOf(Oauth2Exception.class)
                .hasMessageContaining("invalid token");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Parsing and validating Oauth2 client",
                "Refresh token expired hash='%s'".formatted(hash)
        );

        oauthToken.setExpiration(Instant.now().plusSeconds(10));
        val oauthTokenResponse = processor.process(organization, req, res);
        assertLogEntryContainsAndReset(
                accessLogService,
                "Parsing and validating Oauth2 client",
                "Preparing private key for JWT token creation",
                "Signing JWT access token using private key",
                "Inserting JWT access token into DB"
        );
        assertThat(oauthTokenResponse).isNotNull();
        assertThat(oauthTokenResponse.tokenType).isEqualTo("bearer");
        assertThat(oauthTokenResponse.expiresIn).isEqualTo(600);
        assertThat(oauthTokenResponse.refreshToken).isNull();
        assertThat(oauthTokenResponse.scope).isEqualTo(SCOPE);
        assertThat(oauthTokenResponse.accessToken).hasSizeGreaterThan(64);

        val jwt = Jwts.parser()
                .verifyWith(generatePublicKey(oauthClient.getPublicKey()))
                .build()
                .parseSignedClaims(oauthTokenResponse.accessToken);
        assertThat(jwt).isNotNull();
        assertThat(jwt.getHeader()).isNotNull();
        assertThat(jwt.getHeader().get("alg")).isEqualTo("RS384");
        assertThat(jwt.getPayload()).isNotNull();
        assertThat(jwt.getPayload().get("iss")).isEqualTo(organization.getId());
        assertThat(jwt.getPayload().get("sub")).isEqualTo(oauthClient.getId());
        assertThat(jwt.getPayload().get("organization_id")).isEqualTo(organization.getId());
        assertThat(jwt.getPayload().get("iat")).isNotNull();
        assertThat(jwt.getPayload().get("exp")).isNotNull();
        assertThat(jwt.getDigest()).isNotNull();
    }

}