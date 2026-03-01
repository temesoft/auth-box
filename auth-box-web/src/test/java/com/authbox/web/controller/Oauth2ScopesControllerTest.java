package com.authbox.web.controller;

import com.authbox.base.dao.OauthScopeRepository;
import com.authbox.base.model.OauthScope;
import com.authbox.web.Application;
import com.authbox.web.TestUtils;
import com.authbox.web.model.CreateScopeRequest;
import com.authbox.web.model.ScopesRequest;
import com.authbox.web.service.Oauth2ScopesService;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.List;
import java.util.Map;

import static com.authbox.web.TestConstants.VALID_ORGANIZATION_ID;
import static com.authbox.web.TestConstants.VALID_SCOPE_ID;
import static com.authbox.web.config.Constants.API_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = {Application.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureRestTestClient
@TestPropertySource(locations = "classpath:application-test.properties")
class Oauth2ScopesControllerTest {

    @LocalServerPort
    private int port;
    @Autowired
    private RestTestClient restTestClient;
    @Autowired
    private OauthScopeRepository oauthScopeRepository;

    private String jSessionId;

    @BeforeEach
    public void setup() {
        jSessionId = TestUtils.authenticateAccountGetCookie(port);
    }

    @Test
    public void testGetOauth2Scopes() {
        val pageOfClients = restTestClient.get()
                .uri(API_PREFIX + "/oauth2-scope")
                .header("cookie", jSessionId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .returnResult()
                .getResponseBody();
        assertThat(pageOfClients)
                .isNotNull()
                .containsKey("content")
                .containsKey("page");
    }

    @Test
    public void testGetOauth2ScopeById() {
        val scope = restTestClient.get()
                .uri(API_PREFIX + "/oauth2-scope/" + VALID_SCOPE_ID)
                .header("cookie", jSessionId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(OauthScope.class)
                .returnResult()
                .getResponseBody();
        assertThat(scope).isNotNull();
        assertThat(scope.getScope()).isEqualTo("some/scope");
        assertThat(scope.getOrganizationId()).isEqualTo(VALID_ORGANIZATION_ID);
    }

    @Test
    public void testCountClientsUsingScopeId() {
        val scopesRequest = new ScopesRequest(List.of(VALID_SCOPE_ID));
        val scopeCount = restTestClient.post()
                .uri(API_PREFIX + "/oauth2-scope/count-clients")
                .header("cookie", jSessionId)
                .body(scopesRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Integer.class)
                .returnResult()
                .getResponseBody();
        assertThat(scopeCount).isEqualTo(1);
    }

    @Test
    public void testCreateScope() {
        val scope = createScope();
        assertThat(scope).isNotNull();
        assertThat(scope.getScope()).isEqualTo("temp/scope");
        assertThat(scope.getDescription()).isEqualTo("Test scope");
        assertThat(scope.getCreateTime()).isNotNull();
        assertThat(scope.getOrganizationId()).isEqualTo(VALID_ORGANIZATION_ID);
        oauthScopeRepository.deleteById(scope.getId());
    }

    @Test
    public void testUpdateScope() {
        val scope = createScope();
        val oauthScopeDto = Oauth2ScopesService.OauthScopeDto.builder()
                .scope(scope.getScope())
                .description("New description")
                .organizationId(scope.getOrganizationId())
                .build();

        restTestClient.post()
                .uri(API_PREFIX + "/oauth2-scope/" + scope.getId())
                .header("cookie", jSessionId)
                .body(oauthScopeDto)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Oauth2ScopesService.OauthScopeDto.class)
                .returnResult()
                .getResponseBody();

        val updatedScope = restTestClient.get()
                .uri(API_PREFIX + "/oauth2-scope/" + scope.getId())
                .header("cookie", jSessionId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(OauthScope.class)
                .returnResult()
                .getResponseBody();

        assertThat(updatedScope).isNotNull();
        assertThat(updatedScope.getScope()).isEqualTo(scope.getScope());
        assertThat(updatedScope.getDescription()).isNotEqualTo(scope.getScope()).isEqualTo("New description");
        oauthScopeRepository.deleteById(scope.getId());
    }

    @Test
    public void testDeleteScope() {
        val scope = createScope();
        val scopesRequest = new ScopesRequest(List.of(scope.getId()));
        restTestClient.method(HttpMethod.DELETE)
                .uri(API_PREFIX + "/oauth2-scope")
                .header("cookie", jSessionId)
                .body(scopesRequest)
                .exchange()
                .expectStatus().isOk();

        restTestClient.get()
                .uri(API_PREFIX + "/oauth2-scope/" + scope.getId())
                .header("cookie", jSessionId)
                .exchange()
                .expectStatus().isNotFound();
    }

    private Oauth2ScopesService.OauthScopeDto createScope() {
        val createScopeRequest = new CreateScopeRequest("temp/scope", "Test scope");
        val scope = restTestClient.post()
                .uri(API_PREFIX + "/oauth2-scope")
                .header("cookie", jSessionId)
                .body(createScopeRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Oauth2ScopesService.OauthScopeDto.class)
                .returnResult()
                .getResponseBody();
        assertThat(scope).isNotNull();
        return scope;
    }
}