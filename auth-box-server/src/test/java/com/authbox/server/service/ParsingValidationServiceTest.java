package com.authbox.server.service;

import com.authbox.base.dao.OauthClientDao;
import com.authbox.base.exception.BadRequestException;
import com.authbox.base.model.OauthClient;
import com.authbox.base.model.Organization;
import com.authbox.base.service.AccessLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.Optional;

import static com.authbox.base.config.Constants.HEADER_AUTHORIZATION;
import static com.authbox.base.config.Constants.HEADER_AUTHORIZATION_PREFIX_BASIC;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_CLIENT_ID;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_CLIENT_SECRET;
import static com.authbox.base.util.IdUtils.createId;
import static com.authbox.server.TestUtils.assertLogEntryContainsAndReset;
import static com.authbox.server.filter.RequestWrapperFilter.REQUEST_START_REQUEST_TIME_MDC_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ParsingValidationServiceTest {

    private ParsingValidationService service;
    private OauthClientDao oauthClientDao;
    private AccessLogService accessLogService;

    @BeforeEach
    public void setup() {
        oauthClientDao = mock(OauthClientDao.class);
        accessLogService = mock(AccessLogService.class);
        service = new ParsingValidationServiceImpl(oauthClientDao, accessLogService);
        MDC.put(REQUEST_START_REQUEST_TIME_MDC_KEY, System.currentTimeMillis() + "");
    }

    @AfterEach
    public void teardown() {
        MDC.clear();
    }

    @Test
    public void testGetOauthClient() {
        val req = mock(HttpServletRequest.class);
        val organization = Organization.builder()
                .withId(createId())
                .withCreateTime(Instant.now())
                .build();

        assertThatThrownBy(() -> service.getOauthClient(req, organization))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid request");
        assertLogEntryContainsAndReset(accessLogService, "Request missing client credentials");

        val clientId = createId();
        val clientSecret = createId();
        when(req.getParameter(OAUTH2_ATTR_CLIENT_ID)).thenReturn(clientId);
        when(req.getParameter(OAUTH2_ATTR_CLIENT_SECRET)).thenReturn(clientSecret);
        assertThatThrownBy(() -> service.getOauthClient(req, organization))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid request");
        assertLogEntryContainsAndReset(accessLogService, "Oauth2 client not found by id");

        val oauthClient = OauthClient.builder()
                .withId(clientId)
                .withCreateTime(Instant.now())
                .withSecret("bad-secret")
                .withEnabled(false)
                .build();
        when(oauthClientDao.getById(clientId)).thenReturn(Optional.of(oauthClient));
        assertThatThrownBy(() -> service.getOauthClient(req, organization))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid request");
        assertLogEntryContainsAndReset(accessLogService, "Oauth2 client is disabled");

        oauthClient.setEnabled(true);
        oauthClient.setOrganizationId("bad-org-id");
        assertThatThrownBy(() -> service.getOauthClient(req, organization))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid request");
        assertLogEntryContainsAndReset(accessLogService, "Oauth2 client organization details do not match domain prefix specified in request");

        oauthClient.setOrganizationId(organization.getId());
        assertThatThrownBy(() -> service.getOauthClient(req, organization))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid request");
        assertLogEntryContainsAndReset(accessLogService, "Oauth2 client secret does not match provided value");

        oauthClient.setSecret(clientSecret);
        assertThat(service.getOauthClient(req, organization)).isEqualTo(oauthClient);
    }

    @Test
    public void testGetCredentialsFromBasicAuthHeader() {
        val req = mock(HttpServletRequest.class);
        assertThat(service.getCredentialsFromBasicAuthHeader(req)).isEmpty();

        when(req.getHeader(HEADER_AUTHORIZATION)).thenReturn(HEADER_AUTHORIZATION_PREFIX_BASIC + "bad-base64-text");
        assertThat(service.getCredentialsFromBasicAuthHeader(req)).isEmpty();

        when(req.getHeader(HEADER_AUTHORIZATION)).thenReturn(HEADER_AUTHORIZATION_PREFIX_BASIC + "dGVzdGluZw==");
        assertThat(service.getCredentialsFromBasicAuthHeader(req)).isEmpty();

        when(req.getHeader(HEADER_AUTHORIZATION)).thenReturn(HEADER_AUTHORIZATION_PREFIX_BASIC + "dGVzdGluZzp0ZXN0aW5n");
        assertThat(service.getCredentialsFromBasicAuthHeader(req)).isPresent();
    }
}