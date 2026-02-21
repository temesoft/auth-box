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
import com.authbox.base.model.Organization;
import com.authbox.base.service.AccessLogService;
import com.authbox.server.service.ParsingValidationService;
import com.authbox.server.service.ScopeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.authbox.base.util.IdUtils.createId;
import static com.authbox.server.TestUtils.assertLogEntryContainsAndReset;
import static com.authbox.server.filter.RequestWrapperFilter.REQUEST_START_REQUEST_TIME_MDC_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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

    @Test
    public void testGetAuthorize() {
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
}