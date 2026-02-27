package com.authbox.server.service.processor;

import com.authbox.base.dao.OauthClientDao;
import com.authbox.base.dao.OauthTokenDao;
import com.authbox.base.dao.OauthUserDao;
import com.authbox.base.dao.OrganizationDao;
import com.authbox.base.exception.BadRequestException;
import com.authbox.base.exception.Oauth2Exception;
import com.authbox.base.model.OauthClient;
import com.authbox.base.model.OauthToken;
import com.authbox.base.model.OauthUser;
import com.authbox.base.model.Organization;
import com.authbox.base.model.TokenFormat;
import com.authbox.base.model.TokenType;
import com.authbox.base.service.AccessLogService;
import com.authbox.server.service.ParsingValidationService;
import com.authbox.server.service.ParsingValidationServiceImpl;
import com.authbox.server.service.TokenEndpointProcessor;
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
import static com.authbox.base.config.Constants.OAUTH2_ATTR_CODE;
import static com.authbox.base.util.HashUtils.sha256;
import static com.authbox.base.util.IdUtils.createId;
import static com.authbox.server.TestUtils.assertLogEntryContainsAndReset;
import static com.authbox.server.filter.RequestWrapperFilter.REQUEST_START_REQUEST_TIME_MDC_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthorizationCodeGrantTypeTokenEndpointProcessorTest {

    private static final String SCOPE = "some/scope";
    private static final String OAUTH_CLIENT_ID = createId();
    private static final String OAUTH_CLIENT_SECRET = createId();

    private AuthorizationCodeGrantTypeTokenEndpointProcessor processor;
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
        processor = new AuthorizationCodeGrantTypeTokenEndpointProcessor();
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
    public void testProcess() {
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

        val organizationDao = mock(OrganizationDao.class);
        when(organizationDao.getByDomainPrefix("some.domain")).thenReturn(Optional.of(organization));
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
                "Authorization code is missing or empty"
        );

        val code = createId();
        when(req.getParameter(OAUTH2_ATTR_CODE)).thenReturn(code);
        val hash = sha256(code);
        assertThatThrownBy(() -> processor.process(organization, req, res))
                .isInstanceOf(Oauth2Exception.class)
                .hasMessageContaining("unauthorized request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Parsing and validating Oauth2 client",
                "Authorization code='%s', hash='%s' was not found".formatted(code, hash)
        );

        val oauthUser = OauthUser.builder()
                .withId(createId())
                .withCreateTime(Instant.now())
                .withUsername("username")
                .withOrganizationId(organization.getId())
                .build();
        val oauthToken = OauthToken.builder()
                .withId(createId())
                .withCreateTime(Instant.now())
                .withScopes(List.of("some/scope"))
                .withHash(hash)
                .withClientId("clientId")
                .withOauthUserId(null)
                .withOrganizationId("orgId")
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
                "Provided token is not %s. type='%s' token='%s' / hash='%s'"
                        .formatted(TokenType.AUTHORIZATION_CODE, TokenType.ACCESS_TOKEN, code, hash)
        );

        oauthToken.setTokenType(TokenType.AUTHORIZATION_CODE);
        assertThatThrownBy(() -> processor.process(organization, req, res))
                .isInstanceOf(Oauth2Exception.class)
                .hasMessageContaining("invalid token");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Parsing and validating Oauth2 client",
                "Provided authorization code token is already used. hash='%s'".formatted(hash)
        );

        oauthToken.setLinkedTokenId(null);
        assertThatThrownBy(() -> processor.process(organization, req, res))
                .isInstanceOf(Oauth2Exception.class)
                .hasMessageContaining("invalid token");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Parsing and validating Oauth2 client",
                "Authorization code expired code='%s', hash='%s'".formatted(code, hash)
        );

        oauthToken.setExpiration(Instant.now().plusSeconds(10));
        assertThatThrownBy(() -> processor.process(organization, req, res))
                .isInstanceOf(Oauth2Exception.class)
                .hasMessageContaining("unauthorized request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Parsing and validating Oauth2 client",
                "Authorization code user id not available"
        );

        oauthToken.setOauthUserId(oauthUser.getId());
        assertThatThrownBy(() -> processor.process(organization, req, res))
                .isInstanceOf(Oauth2Exception.class)
                .hasMessageContaining("unauthorized request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Parsing and validating Oauth2 client",
                "Authorization code user not found by id='%s'".formatted(oauthUser.getId())
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
}