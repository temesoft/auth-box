package com.authbox.server.service;

import com.authbox.base.exception.BadRequestException;
import com.authbox.base.model.OauthClient;
import com.authbox.base.model.OauthScope;
import com.authbox.base.service.AccessLogService;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.List;

import static com.authbox.base.util.IdUtils.createId;
import static com.authbox.server.TestUtils.assertLogEntryContains;
import static com.authbox.server.filter.RequestWrapperFilter.REQUEST_START_REQUEST_TIME_MDC_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

class ScopeServiceTest {

    private static final String SCOPE_VALID = "some/scope";
    private static final String SCOPE_INVALID = "other/scope";

    private ScopeService service;
    private AccessLogService accessLogService;

    @BeforeEach
    public void setup() {
        accessLogService = mock(AccessLogService.class);
        service = new ScopeServiceImpl(accessLogService);
        MDC.put(REQUEST_START_REQUEST_TIME_MDC_KEY, System.currentTimeMillis() + "");
    }

    @AfterEach
    public void teardown() {
        MDC.clear();
    }

    @Test
    public void testGetScopeStringBasedOnRequestedAndAllowed() {
        val clientId = createId();
        val clientSecret = createId();
        val oauthClient = OauthClient.builder()
                .withId(clientId)
                .withCreateTime(Instant.now())
                .withSecret(clientSecret)
                .withEnabled(false)
                .withScopes(List.of())
                .build();

        assertThatThrownBy(() -> service.getScopeStringBasedOnRequestedAndAllowed(SCOPE_VALID, oauthClient))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid scope");
        assertLogEntryContains(accessLogService, "Requested scope='" + SCOPE_VALID
                + "' is not found in Oauth2 client scopes=[]");
        reset(accessLogService);

        oauthClient.setScopes(List.of(OauthScope.builder()
                .withScope(SCOPE_INVALID)
                .build()));
        assertThatThrownBy(() -> service.getScopeStringBasedOnRequestedAndAllowed(SCOPE_VALID, oauthClient))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid scope");
        assertLogEntryContains(accessLogService, "Requested scope='" + SCOPE_VALID
                + "' is not found in Oauth2 client scopes=[" + SCOPE_INVALID + "]");
        reset(accessLogService);

        oauthClient.setScopes(List.of(OauthScope.builder()
                .withScope(SCOPE_VALID)
                .build()));

        val scopeString = service.getScopeStringBasedOnRequestedAndAllowed(SCOPE_VALID, oauthClient);
        assertThat(scopeString).isEqualTo(SCOPE_VALID);
    }
}