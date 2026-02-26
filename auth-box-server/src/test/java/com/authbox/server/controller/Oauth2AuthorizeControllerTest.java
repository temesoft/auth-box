package com.authbox.server.controller;

import com.authbox.base.config.AppProperties;
import com.authbox.base.dao.OauthClientDao;
import com.authbox.base.dao.OauthTokenDao;
import com.authbox.base.dao.OauthUserDao;
import com.authbox.base.dao.OrganizationDao;
import com.authbox.base.exception.BadRequestException;
import com.authbox.base.model.AuthorizationResponseType;
import com.authbox.base.model.OauthClient;
import com.authbox.base.model.OauthScope;
import com.authbox.base.model.OauthUser;
import com.authbox.base.model.Organization;
import com.authbox.base.service.AccessLogService;
import com.authbox.server.service.ParsingValidationService;
import com.authbox.server.service.ScopeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.val;
import org.jboss.aerogear.security.otp.Totp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.authbox.base.config.Constants.OAUTH2_ATTR_PASSWORD;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_USERNAME;
import static com.authbox.base.util.IdUtils.createId;
import static com.authbox.server.TestUtils.assertLogEntryContainsAndReset;
import static com.authbox.server.controller.Oauth2AuthorizeController.TWO_FACTOR_AUTH_SUCCESS_ATTRIBUTE;
import static com.authbox.server.filter.RequestWrapperFilter.REQUEST_START_REQUEST_TIME_MDC_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class Oauth2AuthorizeControllerTest {

    private static final String SCOPE = "some/scope";

    private Oauth2AuthorizeController authorizeController;
    private OauthClientDao oauthClientDao;
    private OauthUserDao oauthUserDao;
    private OauthTokenDao oauthTokenDao;
    private ScopeService scopeService;
    private AccessLogService accessLogService;
    private OrganizationDao organizationDao;
    private ParsingValidationService parsingValidationService;
    private HttpServletRequest req;
    private HttpServletResponse res;

    @BeforeEach
    public void setup() {
        oauthClientDao = mock(OauthClientDao.class);
        oauthUserDao = mock(OauthUserDao.class);
        oauthTokenDao = mock(OauthTokenDao.class);
        scopeService = mock(ScopeService.class);
        val passwordEncoder = new BCryptPasswordEncoder();
        val defaultClock = Clock.systemUTC();
        authorizeController = new Oauth2AuthorizeController(
                oauthClientDao,
                oauthUserDao,
                passwordEncoder,
                defaultClock,
                oauthTokenDao,
                scopeService
        );
        accessLogService = mock(AccessLogService.class);
        organizationDao = mock(OrganizationDao.class);
        parsingValidationService = mock(ParsingValidationService.class);
        authorizeController.setAppProperties(new AppProperties());
        authorizeController.setAccessLogService(accessLogService);
        authorizeController.setOrganizationDao(organizationDao);
        authorizeController.setParsingValidationService(parsingValidationService);
        req = mock(HttpServletRequest.class);
        res = mock(HttpServletResponse.class);
        MDC.put(REQUEST_START_REQUEST_TIME_MDC_KEY, System.currentTimeMillis() + "");
    }

    @AfterEach
    public void teardown() {
        MDC.clear();
    }

    @ParameterizedTest
    @MethodSource("authorizationResponseTypes")
    public void testGetAuthorize(final AuthorizationResponseType authorizationResponseType) {
        val clientId = createId();
        val redirectUrl = "http://some.domain/redirect";
        when(req.getRequestURL()).thenReturn(new StringBuffer("http://some.domain/auth"));

        assertThatThrownBy(() -> authorizeController.getAuthorize(
                req,
                AuthorizationResponseType.code,
                clientId,
                redirectUrl,
                "state",
                "some/scope"
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Domain prefix unknown: some.domain");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Starting Oauth2 authorization process",
                "Organization not found by domain prefix='some.domain'"
        );

        val organization = Organization.builder()
                .withId(createId())
                .withCreateTime(Instant.now())
                .withDomainPrefix("some.domain")
                .withEnabled(false)
                .withName("Some Organization")
                .build();
        when(organizationDao.getByDomainPrefix("some.domain")).thenReturn(Optional.of(organization));
        assertThatThrownBy(() -> authorizeController.getAuthorize(
                req,
                AuthorizationResponseType.code,
                clientId,
                redirectUrl,
                "state",
                "some/scope"
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Domain is disabled: some.domain");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Starting Oauth2 authorization process",
                "Organization with prefix='some.domain' is disabled"
        );

        organization.setEnabled(true);
        assertThatThrownBy(() -> authorizeController.getAuthorize(
                req,
                AuthorizationResponseType.code,
                clientId,
                redirectUrl,
                "state",
                "some/scope"
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Starting Oauth2 authorization process",
                "Oauth2 client not found by id='%s'".formatted(clientId)
        );

        val oauthClient = OauthClient.builder()
                .withId(clientId)
                .withCreateTime(Instant.now())
                .withSecret("bad-secret")
                .withEnabled(false)
                .withOrganizationId("bad-org-id")
                .withRedirectUrls(List.of("http://bad-redirect-url"))
                .withScopes(List.of())
                .build();
        when(oauthClientDao.getById(clientId)).thenReturn(Optional.of(oauthClient));
        assertThatThrownBy(() -> authorizeController.getAuthorize(
                req,
                AuthorizationResponseType.code,
                clientId,
                redirectUrl,
                "state",
                "some/scope"
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Starting Oauth2 authorization process",
                "Oauth2 client organization_id='%s' does not match domain prefix specified organization id='%s'"
                        .formatted("bad-org-id", organization.getId())
        );

        oauthClient.setOrganizationId(organization.getId());
        assertThatThrownBy(() -> authorizeController.getAuthorize(
                req,
                AuthorizationResponseType.code,
                clientId,
                redirectUrl,
                "state",
                "some/scope"
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Starting Oauth2 authorization process",
                "Oauth2 client is disabled id='%s'".formatted(clientId)
        );

        when(scopeService.getScopeStringBasedOnRequestedAndAllowed(any(), any())).thenReturn(SCOPE);
        oauthClient.setEnabled(true);
        oauthClient.setScopes(List.of(OauthScope.builder()
                .withScope(SCOPE)
                .build()));
        authorizeController.getAuthorize(
                req,
                AuthorizationResponseType.code,
                clientId,
                redirectUrl,
                "state",
                "some/scope"
        );
        assertLogEntryContainsAndReset(
                accessLogService,
                "Starting Oauth2 authorization process",
                ("Oauth2 client approved redirect urls=[http://bad-redirect-url] " +
                        "does not match requested redirect url='%s'").formatted(redirectUrl)
        );

        oauthClient.setRedirectUrls(List.of(redirectUrl));
        val modelAndView = authorizeController.getAuthorize(
                req,
                AuthorizationResponseType.code,
                clientId,
                redirectUrl,
                "state",
                "some/scope"
        );
        assertLogEntryContainsAndReset(
                accessLogService,
                "Starting Oauth2 authorization process",
                "Displaying authorize HTML page"
        );
        assertThat(modelAndView).isNotNull();
        assertThat(modelAndView.getViewName()).isEqualTo("authorize");
        assertThat(modelAndView.getModel()).hasSize(7);
    }

    @ParameterizedTest
    @MethodSource("authorizationResponseTypesAnd2Fa")
    public void testAuthorizeUserCredentials(final AuthorizationResponseType authorizationResponseType,
                                             final Boolean is2Fa) throws IOException {
        val clientId = createId();
        val redirectUrl = "http://some.domain/redirect";
        val username = "username";
        val password = "password";
        when(req.getRequestURL()).thenReturn(new StringBuffer("http://some.domain/auth"));

        assertThatThrownBy(() -> authorizeController.authorizeUserCredentials(
                req,
                res,
                authorizationResponseType,
                clientId,
                redirectUrl,
                "state",
                "some/scope",
                username,
                password
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Domain prefix unknown: some.domain");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Starting Oauth2 authorization process (validate credentials)",
                "Organization not found by domain prefix='some.domain'"
        );

        val organization = Organization.builder()
                .withId(createId())
                .withCreateTime(Instant.now())
                .withDomainPrefix("some.domain")
                .withEnabled(false)
                .withName("Some Organization")
                .build();
        when(organizationDao.getByDomainPrefix("some.domain")).thenReturn(Optional.of(organization));
        assertThatThrownBy(() -> authorizeController.authorizeUserCredentials(
                req,
                res,
                authorizationResponseType,
                clientId,
                redirectUrl,
                "state",
                "some/scope",
                username,
                password
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Domain is disabled: some.domain");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Starting Oauth2 authorization process (validate credentials)",
                "Organization with prefix='some.domain' is disabled"
        );

        organization.setEnabled(true);
        assertThatThrownBy(() -> authorizeController.authorizeUserCredentials(
                req,
                res,
                authorizationResponseType,
                clientId,
                redirectUrl,
                "state",
                "some/scope",
                username,
                password
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Starting Oauth2 authorization process (validate credentials)",
                "Oauth2 client not found by id='%s'".formatted(clientId)
        );

        val oauthClient = OauthClient.builder()
                .withId(clientId)
                .withCreateTime(Instant.now())
                .withSecret("bad-secret")
                .withEnabled(false)
                .withOrganizationId("bad-org-id")
                .withRedirectUrls(List.of("http://bad-redirect-url"))
                .withScopes(List.of())
                .build();
        when(oauthClientDao.getById(clientId)).thenReturn(Optional.of(oauthClient));
        assertThatThrownBy(() -> authorizeController.authorizeUserCredentials(
                req,
                res,
                authorizationResponseType,
                clientId,
                redirectUrl,
                "state",
                "some/scope",
                username,
                password
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Starting Oauth2 authorization process (validate credentials)",
                "Oauth2 client organization id='%s' does not match domain prefix specified organization id='%s'"
                        .formatted("bad-org-id", organization.getId())
        );

        assertThatThrownBy(() -> authorizeController.authorizeUserCredentials(
                req,
                res,
                authorizationResponseType,
                clientId,
                redirectUrl,
                "", // empty state
                "some/scope",
                username,
                password
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Starting Oauth2 authorization process (validate credentials)",
                "Authorization state parameter is not provided"
        );

        oauthClient.setOrganizationId(organization.getId());
        assertThatThrownBy(() -> authorizeController.authorizeUserCredentials(
                req,
                res,
                authorizationResponseType,
                clientId,
                redirectUrl,
                "state",
                "some/scope",
                username,
                password
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Starting Oauth2 authorization process (validate credentials)",
                "Oauth2 client is disabled id='%s'".formatted(clientId)
        );

        when(scopeService.getScopeStringBasedOnRequestedAndAllowed(any(), any())).thenReturn(SCOPE);
        oauthClient.setEnabled(true);
        oauthClient.setScopes(List.of(OauthScope.builder()
                .withScope(SCOPE)
                .build()));
        authorizeController.authorizeUserCredentials(
                req,
                res,
                authorizationResponseType,
                clientId,
                redirectUrl,
                "state",
                "some/scope",
                username,
                password
        );
        assertLogEntryContainsAndReset(
                accessLogService,
                "Starting Oauth2 authorization process (validate credentials)",
                ("Oauth2 client approved redirect urls=[http://bad-redirect-url] " +
                        "does not match requested redirect url='%s'").formatted(redirectUrl)
        );

        oauthClient.setRedirectUrls(List.of(redirectUrl));
        authorizeController.authorizeUserCredentials(
                req,
                res,
                authorizationResponseType,
                clientId,
                redirectUrl,
                "state",
                "some/scope",
                username,
                password
        );
        assertLogEntryContainsAndReset(
                accessLogService,
                "Starting Oauth2 authorization process (validate credentials)",
                "Oauth2 user not found by username='%s' and organization id='%s'"
                        .formatted(username, organization.getId())
        );

        val oauthUser = OauthUser.builder()
                .withId(createId())
                .withCreateTime(Instant.now())
                .withOrganizationId("bad-org-id")
                .withUsername(username)
                .withPassword(password)
                .withUsing2Fa(is2Fa)
                .build();
        when(oauthUserDao.getByUsernameAndOrganizationId(username, organization.getId()))
                .thenReturn(Optional.of(oauthUser));
        authorizeController.authorizeUserCredentials(
                req,
                res,
                authorizationResponseType,
                clientId,
                redirectUrl,
                "state",
                "some/scope",
                username,
                password
        );
        assertLogEntryContainsAndReset(
                accessLogService,
                "Starting Oauth2 authorization process (validate credentials)",
                "Oauth2 user is disabled; username='%s' and organization id='%s'"
                        .formatted(username, organization.getId())
        );

        oauthUser.setEnabled(true);
        assertThatThrownBy(() -> authorizeController.authorizeUserCredentials(
                req,
                res,
                authorizationResponseType,
                clientId,
                redirectUrl,
                "state",
                "some/scope",
                username,
                password
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Starting Oauth2 authorization process (validate credentials)",
                "Oauth2 user organization id='bad-org-id' does not match request organization id='%s'"
                        .formatted(organization.getId())
        );

        oauthUser.setOrganizationId(organization.getId());
        authorizeController.authorizeUserCredentials(
                req,
                res,
                authorizationResponseType,
                clientId,
                redirectUrl,
                "state",
                "some/scope",
                username,
                "bad-password"
        );
        assertLogEntryContainsAndReset(
                accessLogService,
                "Starting Oauth2 authorization process (validate credentials)",
                "Oauth2 user password does not match request password"
        );

        oauthUser.setPassword(new BCryptPasswordEncoder().encode(password));
        val session = mock(HttpSession.class);
        when(req.getSession()).thenReturn(session);
        when(session.getAttribute(OAUTH2_ATTR_USERNAME)).thenReturn(username);
        when(session.getAttribute(OAUTH2_ATTR_PASSWORD)).thenReturn(password);
        val modelAndView = authorizeController.authorizeUserCredentials(
                req,
                res,
                authorizationResponseType,
                clientId,
                redirectUrl,
                "state",
                "some/scope",
                username,
                password
        );
        if (is2Fa) {
            assertThat(modelAndView).isNotNull();
            assertThat(modelAndView.getViewName()).isEqualTo("authorize-2fa");
            assertThat(modelAndView.getModel()).hasSize(8);
            assertLogEntryContainsAndReset(
                    accessLogService,
                    "Starting Oauth2 authorization process (validate credentials)",
                    "Displaying Google Authenticator 2FA authorize HTML page"
            );
        } else {
            assertThat(modelAndView).isNull();
            val urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(res, times(1)).sendRedirect(urlCaptor.capture());
            assertThat(urlCaptor.getValue())
                    .contains("http://some.domain/redirect?code=");
            assertLogEntryContainsAndReset(
                    accessLogService,
                    "Starting Oauth2 authorization process (validate credentials)",
                    "Starting Oauth2 authorization process (redirect with authorization code)",
                    "Oauth2 authorization process finished"
            );
        }
    }

    @ParameterizedTest
    @MethodSource("authorizationResponseTypes")
    public void testAuthorizeUser2Fa(final AuthorizationResponseType authorizationResponseType) throws IOException {
        val clientId = createId();
        val redirectUrl = "http://some.domain/redirect";
        val username = "username";
        val password = "password";
        val code2faInvalid = new Totp("bad-secret").now();
        when(req.getRequestURL()).thenReturn(new StringBuffer("http://some.domain/auth"));

        assertThatThrownBy(() -> authorizeController.authorizeUser2Fa(
                req,
                res,
                authorizationResponseType,
                clientId,
                redirectUrl,
                "state",
                "some/scope",
                code2faInvalid
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Domain prefix unknown: some.domain");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Starting Oauth2 authorization process (Google Authenticator 2FA code verification)",
                "Organization not found by domain prefix='some.domain'"
        );

        val organization = Organization.builder()
                .withId(createId())
                .withCreateTime(Instant.now())
                .withDomainPrefix("some.domain")
                .withEnabled(false)
                .withName("Some Organization")
                .build();
        when(organizationDao.getByDomainPrefix("some.domain")).thenReturn(Optional.of(organization));
        assertThatThrownBy(() -> authorizeController.authorizeUser2Fa(
                req,
                res,
                authorizationResponseType,
                clientId,
                redirectUrl,
                "state",
                "some/scope",
                code2faInvalid
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Domain is disabled: some.domain");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Starting Oauth2 authorization process (Google Authenticator 2FA code verification)",
                "Organization with prefix='some.domain' is disabled"
        );

        organization.setEnabled(true);
        assertThatThrownBy(() -> authorizeController.authorizeUser2Fa(
                req,
                res,
                authorizationResponseType,
                clientId,
                redirectUrl,
                "state",
                "some/scope",
                code2faInvalid
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Starting Oauth2 authorization process (Google Authenticator 2FA code verification)",
                "Oauth2 client not found by id='%s'".formatted(clientId)
        );

        val oauthClient = OauthClient.builder()
                .withId(clientId)
                .withCreateTime(Instant.now())
                .withSecret("bad-secret")
                .withEnabled(false)
                .withOrganizationId("bad-org-id")
                .withRedirectUrls(List.of("http://bad-redirect-url"))
                .withScopes(List.of())
                .build();
        when(oauthClientDao.getById(clientId)).thenReturn(Optional.of(oauthClient));
        assertThatThrownBy(() -> authorizeController.authorizeUser2Fa(
                req,
                res,
                authorizationResponseType,
                clientId,
                redirectUrl,
                "state",
                "some/scope",
                code2faInvalid
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Starting Oauth2 authorization process (Google Authenticator 2FA code verification)",
                "Oauth2 client organization id='%s' does not match domain prefix specified organization id='%s'"
                        .formatted("bad-org-id", organization.getId())
        );

        assertThatThrownBy(() -> authorizeController.authorizeUser2Fa(
                req,
                res,
                authorizationResponseType,
                clientId,
                redirectUrl,
                "", // empty state
                "some/scope",
                code2faInvalid
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Starting Oauth2 authorization process (Google Authenticator 2FA code verification)",
                "Authorization state parameter is not provided"
        );

        oauthClient.setOrganizationId(organization.getId());
        assertThatThrownBy(() -> authorizeController.authorizeUser2Fa(
                req,
                res,
                authorizationResponseType,
                clientId,
                redirectUrl,
                "state",
                "some/scope",
                code2faInvalid
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Starting Oauth2 authorization process (Google Authenticator 2FA code verification)",
                "Oauth2 client is disabled id='%s'".formatted(clientId)
        );

        when(scopeService.getScopeStringBasedOnRequestedAndAllowed(any(), any())).thenReturn(SCOPE);
        oauthClient.setEnabled(true);
        oauthClient.setScopes(List.of(OauthScope.builder()
                .withScope(SCOPE)
                .build()));
        authorizeController.authorizeUser2Fa(
                req,
                res,
                authorizationResponseType,
                clientId,
                redirectUrl,
                "state",
                "some/scope",
                code2faInvalid
        );
        assertLogEntryContainsAndReset(
                accessLogService,
                "Starting Oauth2 authorization process (Google Authenticator 2FA code verification)",
                ("Oauth2 client approved redirect urls=[http://bad-redirect-url] " +
                        "does not match requested redirect url='%s'").formatted(redirectUrl)
        );

        val session = mock(HttpSession.class);
        when(req.getSession()).thenReturn(session);
        when(session.getAttribute(OAUTH2_ATTR_USERNAME)).thenReturn(username);
        when(session.getAttribute(OAUTH2_ATTR_PASSWORD)).thenReturn(password);
        oauthClient.setRedirectUrls(List.of(redirectUrl));
        authorizeController.authorizeUser2Fa(
                req,
                res,
                authorizationResponseType,
                clientId,
                redirectUrl,
                "state",
                "some/scope",
                code2faInvalid
        );
        assertLogEntryContainsAndReset(
                accessLogService,
                "Starting Oauth2 authorization process (Google Authenticator 2FA code verification)",
                "Oauth2 user not found by username='%s' and organization id='%s'"
                        .formatted(username, organization.getId())
        );

        val oauthUser = OauthUser.builder()
                .withId(createId())
                .withCreateTime(Instant.now())
                .withOrganizationId("bad-org-id")
                .withUsername(username)
                .withPassword(password)
                .withUsing2Fa(true)
                .withSecret("some-secret")
                .build();
        when(oauthUserDao.getByUsernameAndOrganizationId(username, organization.getId()))
                .thenReturn(Optional.of(oauthUser));
        authorizeController.authorizeUser2Fa(
                req,
                res,
                authorizationResponseType,
                clientId,
                redirectUrl,
                "state",
                "some/scope",
                code2faInvalid
        );
        assertLogEntryContainsAndReset(
                accessLogService,
                "Starting Oauth2 authorization process (Google Authenticator 2FA code verification)",
                "Oauth2 user is disabled; username='%s' and organization id='%s'"
                        .formatted(username, organization.getId())
        );

        oauthUser.setEnabled(true);
        assertThatThrownBy(() -> authorizeController.authorizeUser2Fa(
                req,
                res,
                authorizationResponseType,
                clientId,
                redirectUrl,
                "state",
                "some/scope",
                code2faInvalid
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Starting Oauth2 authorization process (Google Authenticator 2FA code verification)",
                "Oauth2 user organization id='bad-org-id' does not match request organization id='%s'"
                        .formatted(organization.getId())
        );

        oauthUser.setOrganizationId(organization.getId());
        authorizeController.authorizeUser2Fa(
                req,
                res,
                authorizationResponseType,
                clientId,
                redirectUrl,
                "state",
                "some/scope",
                code2faInvalid
        );
        assertLogEntryContainsAndReset(
                accessLogService,
                "Starting Oauth2 authorization process (Google Authenticator 2FA code verification)",
                "User provided invalid Google Authenticator 2FA verification code"
        );

        val code2faValid = new Totp(oauthUser.getSecret()).now();
        authorizeController.authorizeUser2Fa(
                req,
                res,
                authorizationResponseType,
                clientId,
                redirectUrl,
                "state",
                "some/scope",
                code2faValid
        );
        assertLogEntryContainsAndReset(
                accessLogService,
                "Starting Oauth2 authorization process (Google Authenticator 2FA code verification)",
                "Google Authenticator 2FA verification code validated",
                "Starting Oauth2 authorization process (redirect with authorization code)",
                "Oauth2 user password does not match request password"
        );

        oauthUser.setPassword(new BCryptPasswordEncoder().encode(password));
        when(session.getAttribute(TWO_FACTOR_AUTH_SUCCESS_ATTRIBUTE)).thenReturn(code2faValid);
        val modelAndView = authorizeController.authorizeUser2Fa(
                req,
                res,
                authorizationResponseType,
                clientId,
                redirectUrl,
                "state",
                "some/scope",
                code2faValid
        );
        assertThat(modelAndView).isNull();
        val urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(res, times(1)).sendRedirect(urlCaptor.capture());
        assertThat(urlCaptor.getValue())
                .contains("http://some.domain/redirect?code=");
    }

    static Stream<AuthorizationResponseType> authorizationResponseTypes() {
        return Stream.of(AuthorizationResponseType.code, AuthorizationResponseType.token);
    }

    static Stream<Arguments> authorizationResponseTypesAnd2Fa() {
        return Stream.of(
                Arguments.arguments(AuthorizationResponseType.code, true),
                Arguments.arguments(AuthorizationResponseType.code, false),
                Arguments.arguments(AuthorizationResponseType.token, true),
                Arguments.arguments(AuthorizationResponseType.token, false)
        );
    }
}