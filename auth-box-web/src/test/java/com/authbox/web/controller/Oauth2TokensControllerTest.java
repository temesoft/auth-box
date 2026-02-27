package com.authbox.web.controller;

import com.authbox.base.dao.OauthScopeRepository;
import com.authbox.web.Application;
import com.authbox.web.TestUtils;
import com.authbox.web.model.DeleteTokensRequest;
import com.authbox.web.service.Oauth2TokensService;
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

import static com.authbox.base.util.IdUtils.createId;
import static com.authbox.web.TestConstants.VALID_CLIENT_ID;
import static com.authbox.web.TestConstants.VALID_OAUTH_USER_ID;
import static com.authbox.web.TestConstants.VALID_ORGANIZATION_ID;
import static com.authbox.web.TestConstants.VALID_TOKEN;
import static com.authbox.web.TestConstants.VALID_TOKEN_HASH;
import static com.authbox.web.TestConstants.VALID_TOKEN_ID;
import static com.authbox.web.config.Constants.API_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = {Application.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureRestTestClient
@TestPropertySource(locations = "classpath:application-test.properties")
class Oauth2TokensControllerTest {

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
    void testGetOauth2TokensByClientId() {
        val pageOfTokens = restTestClient.get()
                .uri(API_PREFIX + "/oauth2-token/client/" + VALID_CLIENT_ID)
                .header("cookie", jSessionId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .returnResult()
                .getResponseBody();
        assertThat(pageOfTokens)
                .isNotNull()
                .containsKey("content")
                .containsKey("page");
    }

    @Test
    void testGetOauth2TokensByUserId() {
        val pageOfTokens = restTestClient.get()
                .uri(API_PREFIX + "/oauth2-token/user/" + VALID_OAUTH_USER_ID)
                .header("cookie", jSessionId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .returnResult()
                .getResponseBody();
        assertThat(pageOfTokens)
                .isNotNull()
                .containsKey("content")
                .containsKey("page");
    }

    @Test
    void testListOauth2Token() {
        val pageOfTokens = restTestClient.get()
                .uri(API_PREFIX + "/oauth2-token/list")
                .header("cookie", jSessionId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .returnResult()
                .getResponseBody();
        assertThat(pageOfTokens)
                .isNotNull()
                .containsKey("content")
                .containsKey("page");
    }

    @Test
    void testGetOauth2TokenByHash() {
        val token = restTestClient.get()
                .uri(API_PREFIX + "/oauth2-token/hash/" + VALID_TOKEN_HASH)
                .header("cookie", jSessionId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Oauth2TokensService.OauthTokenDto.class)
                .returnResult()
                .getResponseBody();
        assertThat(token).isNotNull();
        assertThat(token.getId()).isEqualTo(VALID_TOKEN_ID);
        assertThat(token.getHash()).isEqualTo(VALID_TOKEN_HASH);
        assertThat(token.getOrganizationId()).isEqualTo(VALID_ORGANIZATION_ID);
        assertThat(token.getClientId()).isEqualTo(VALID_CLIENT_ID);
    }

    @Test
    void testGetOauth2TokenByToken() {
        val token = restTestClient.get()
                .uri(API_PREFIX + "/oauth2-token/token/" + VALID_TOKEN)
                .header("cookie", jSessionId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Oauth2TokensService.OauthTokenDto.class)
                .returnResult()
                .getResponseBody();
        assertThat(token).isNotNull();
        assertThat(token.getId()).isEqualTo(VALID_TOKEN_ID);
        assertThat(token.getHash()).isEqualTo(VALID_TOKEN_HASH);
        assertThat(token.getOrganizationId()).isEqualTo(VALID_ORGANIZATION_ID);
        assertThat(token.getClientId()).isEqualTo(VALID_CLIENT_ID);
    }

    @Test
    void testGetOauth2TokenById() {
        val token = restTestClient.get()
                .uri(API_PREFIX + "/oauth2-token/id/" + VALID_TOKEN_ID)
                .header("cookie", jSessionId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Oauth2TokensService.OauthTokenDto.class)
                .returnResult()
                .getResponseBody();
        assertThat(token).isNotNull();
        assertThat(token.getId()).isEqualTo(VALID_TOKEN_ID);
        assertThat(token.getHash()).isEqualTo(VALID_TOKEN_HASH);
        assertThat(token.getOrganizationId()).isEqualTo(VALID_ORGANIZATION_ID);
        assertThat(token.getClientId()).isEqualTo(VALID_CLIENT_ID);
    }

    @Test
    void deleteOauth2Tokens() {
        val deleteTokensRequest = new DeleteTokensRequest(List.of(createId()));
        restTestClient.method(HttpMethod.DELETE)
                .uri(API_PREFIX + "/oauth2-token")
                .header("cookie", jSessionId)
                .body(deleteTokensRequest)
                .exchange()
                .expectStatus().isNotFound();
    }
}