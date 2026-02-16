package com.authbox.server.controller;

import com.authbox.base.model.ErrorResponse;
import com.authbox.base.model.OauthTokenResponse;
import com.authbox.server.Application;
import com.authbox.server.TestConstants;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

import static com.authbox.base.config.Constants.MSG_INVALID_REQUEST;
import static com.authbox.base.config.Constants.MSG_INVALID_SCOPE;
import static com.authbox.base.config.Constants.OAUTH_PREFIX;
import static com.authbox.base.model.GrantType.password;
import static com.authbox.server.TestConstants.VALID_CLIENT_ID;
import static com.authbox.server.TestConstants.VALID_CLIENT_SECRET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;

@SpringBootTest(
        classes = {Application.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureRestTestClient
@TestPropertySource(locations = "classpath:application-test.properties")
public class Oauth2TokenControllerWithPasswordTest {

    @LocalServerPort
    private int port;

    @Autowired
    private RestTestClient restClient;

    @Test
    public void testCreateOauth2Token_Success_UsingAuthHeader() {
        val response = restClient.post()
                .uri(builder ->
                        builder.path(OAUTH_PREFIX + "/token")
                                .queryParam("grant_type", password.name())
                                .queryParam("username", TestConstants.VALID_USERNAME)
                                .queryParam("password", TestConstants.VALID_PASSWORD)
                                .queryParam("scope", "some/scope another/scope")
                                .build()
                )
                .header("content-type", APPLICATION_FORM_URLENCODED_VALUE)
                .headers(h -> h.setBasicAuth(VALID_CLIENT_ID, VALID_CLIENT_SECRET))
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(OauthTokenResponse.class)
                .returnResult().getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.accessToken).isNotBlank();
        assertThat(response.tokenType).isEqualTo("bearer");
        assertThat(response.expiresIn).isEqualTo(3600);
        assertThat(response.refreshToken).isNotBlank();
        assertThat(response.scope).contains("some/scope").contains("another/scope");
    }

    @Test
    public void testCreateOauth2Token_Success_UsingParams() {
        val response = restClient.post()
                .uri(builder ->
                        builder.path(OAUTH_PREFIX + "/token")
                                .queryParam("grant_type", password.name())
                                .queryParam("username", TestConstants.VALID_USERNAME)
                                .queryParam("password", TestConstants.VALID_PASSWORD)
                                .queryParam("scope", "some/scope another/scope")
                                .queryParam("client_id", TestConstants.VALID_CLIENT_ID)
                                .queryParam("client_secret", TestConstants.VALID_CLIENT_SECRET)
                                .build()
                )
                .header("content-type", APPLICATION_FORM_URLENCODED_VALUE)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(OauthTokenResponse.class)
                .returnResult().getResponseBody();
        assertThat(response).isNotNull();
        assertThat(response.accessToken).isNotBlank();
        assertThat(response.tokenType).isEqualTo("bearer");
        assertThat(response.expiresIn).isEqualTo(3600);
        assertThat(response.refreshToken).isNotBlank();
        assertThat(response.scope).contains("some/scope").contains("another/scope");
    }

    @Test
    public void testCreateOauth2Token_Success_LessScope() {
        val response = restClient.post()
                .uri(builder ->
                        builder.path(OAUTH_PREFIX + "/token")
                                .queryParam("grant_type", password.name())
                                .queryParam("username", TestConstants.VALID_USERNAME)
                                .queryParam("password", TestConstants.VALID_PASSWORD)
                                .queryParam("scope", "some/scope")
                                .queryParam("client_id", TestConstants.VALID_CLIENT_ID)
                                .queryParam("client_secret", TestConstants.VALID_CLIENT_SECRET)
                                .build()
                )
                .header("content-type", APPLICATION_FORM_URLENCODED_VALUE)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(OauthTokenResponse.class)
                .returnResult().getResponseBody();
        assertThat(response).isNotNull();
        assertThat(response.accessToken).isNotBlank();
        assertThat(response.tokenType).isEqualTo("bearer");
        assertThat(response.expiresIn).isEqualTo(3600);
        assertThat(response.refreshToken).isNotBlank();
        assertThat(response.scope).isEqualTo("some/scope");
    }

    @Test
    public void testCreateOauth2Token_Success_NoScopeSpecified() {
        val response = restClient.post()
                .uri(builder ->
                        builder.path(OAUTH_PREFIX + "/token")
                                .queryParam("grant_type", password.name())
                                .queryParam("username", TestConstants.VALID_USERNAME)
                                .queryParam("password", TestConstants.VALID_PASSWORD)
                                .queryParam("client_id", TestConstants.VALID_CLIENT_ID)
                                .queryParam("client_secret", TestConstants.VALID_CLIENT_SECRET)
                                .build()
                )
                .header("content-type", APPLICATION_FORM_URLENCODED_VALUE)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(OauthTokenResponse.class)
                .returnResult().getResponseBody();
        assertThat(response).isNotNull();
        assertThat(response.accessToken).isNotBlank();
        assertThat(response.tokenType).isEqualTo("bearer");
        assertThat(response.expiresIn).isEqualTo(3600);
        assertThat(response.refreshToken).isNotBlank();
        assertThat(response.scope).isNull();
    }

    @Test
    public void testCreateOauth2Token_Failure_BadUsername() {
        val response = restClient.post()
                .uri(builder ->
                        builder.path(OAUTH_PREFIX + "/token")
                                .queryParam("grant_type", password.name())
                                .queryParam("username", "bad-username")
                                .queryParam("password", TestConstants.VALID_PASSWORD)
                                .queryParam("scope", "some/scope")
                                .queryParam("client_id", TestConstants.VALID_CLIENT_ID)
                                .queryParam("client_secret", TestConstants.VALID_CLIENT_SECRET)
                                .build()
                )
                .header("content-type", APPLICATION_FORM_URLENCODED_VALUE)
                .exchange()
                .expectStatus().is4xxClientError()
                .expectBody(ErrorResponse.class)
                .returnResult().getResponseBody();
        assertThat(response).isNotNull();
        assertThat(response.timestamp).isNotNull();
        assertThat(response).isEqualTo(
                new ErrorResponse(
                        response.timestamp,
                        400,
                        "Bad Request",
                        MSG_INVALID_REQUEST,
                        "/oauth/token"
                )
        );
    }

    @Test
    public void testCreateOauth2Token_Failure_BadPassword() {
        val response = restClient.post()
                .uri(builder ->
                        builder.path(OAUTH_PREFIX + "/token")
                                .queryParam("grant_type", password.name())
                                .queryParam("username", TestConstants.VALID_USERNAME)
                                .queryParam("password", "bad-password")
                                .queryParam("scope", "some/scope")
                                .queryParam("client_id", TestConstants.VALID_CLIENT_ID)
                                .queryParam("client_secret", TestConstants.VALID_CLIENT_SECRET)
                                .build()
                )
                .header("content-type", APPLICATION_FORM_URLENCODED_VALUE)
                .exchange()
                .expectStatus().is4xxClientError()
                .expectBody(ErrorResponse.class)
                .returnResult().getResponseBody();
        assertThat(response).isNotNull();
        assertThat(response.timestamp).isNotNull();
        assertThat(response).isEqualTo(
                new ErrorResponse(
                        response.timestamp,
                        400,
                        "Bad Request",
                        MSG_INVALID_REQUEST,
                        "/oauth/token"
                )
        );
    }

    @Test
    public void testCreateOauth2Token_Failure_BadScope() {
        val response = restClient.post()
                .uri(builder ->
                        builder.path(OAUTH_PREFIX + "/token")
                                .queryParam("grant_type", password.name())
                                .queryParam("username", TestConstants.VALID_USERNAME)
                                .queryParam("password", TestConstants.VALID_PASSWORD)
                                .queryParam("scope", "scope/NotOnAllowList")
                                .queryParam("client_id", TestConstants.VALID_CLIENT_ID)
                                .queryParam("client_secret", TestConstants.VALID_CLIENT_SECRET)
                                .build()
                )
                .header("content-type", APPLICATION_FORM_URLENCODED_VALUE)
                .exchange()
                .expectStatus().is4xxClientError()
                .expectBody(ErrorResponse.class)
                .returnResult().getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.timestamp).isNotNull();
        assertThat(response).isEqualTo(
                new ErrorResponse(
                        response.timestamp,
                        400,
                        "Bad Request",
                        MSG_INVALID_SCOPE,
                        "/oauth/token"
                )
        );
    }

    @Test
    public void testCreateOauth2Token_Failure_BadClientId() {
        val response = restClient.post()
                .uri(builder ->
                        builder.path(OAUTH_PREFIX + "/token")
                                .queryParam("grant_type", password.name())
                                .queryParam("username", TestConstants.VALID_USERNAME)
                                .queryParam("password", TestConstants.VALID_PASSWORD)
                                .queryParam("scope", "some/scope")
                                .queryParam("client_id", "bad-client-id")
                                .queryParam("client_secret", TestConstants.VALID_CLIENT_SECRET)
                                .build()
                )
                .header("content-type", APPLICATION_FORM_URLENCODED_VALUE)
                .exchange()
                .expectStatus().is4xxClientError()
                .expectBody(ErrorResponse.class)
                .returnResult().getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.timestamp).isNotNull();
        assertThat(response).isEqualTo(
                new ErrorResponse(
                        response.timestamp,
                        400,
                        "Bad Request",
                        MSG_INVALID_REQUEST,
                        "/oauth/token"
                )
        );
    }

    @Test
    public void testCreateOauth2Token_Failure_BadClientSecret() {
        val response = restClient.post()
                .uri(builder ->
                        builder.path(OAUTH_PREFIX + "/token")
                                .queryParam("grant_type", password.name())
                                .queryParam("username", TestConstants.VALID_USERNAME)
                                .queryParam("password", TestConstants.VALID_PASSWORD)
                                .queryParam("scope", "some/scope")
                                .queryParam("client_id", TestConstants.VALID_CLIENT_ID)
                                .queryParam("client_secret", "this-is-a-wrong-secret")
                                .build()
                )
                .header("content-type", APPLICATION_FORM_URLENCODED_VALUE)
                .exchange()
                .expectStatus().is4xxClientError()
                .expectBody(ErrorResponse.class)
                .returnResult().getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.timestamp).isNotNull();
        assertThat(response).isEqualTo(
                new ErrorResponse(
                        response.timestamp,
                        400,
                        "Bad Request",
                        MSG_INVALID_REQUEST,
                        "/oauth/token"
                )
        );
    }

    @Test
    public void testCreateOauth2Token_Failure_NoClientSecret() {
        val response = restClient.post()
                .uri(builder ->
                        builder.path(OAUTH_PREFIX + "/token")
                                .queryParam("grant_type", password.name())
                                .queryParam("username", TestConstants.VALID_USERNAME)
                                .queryParam("password", TestConstants.VALID_PASSWORD)
                                .queryParam("scope", "some/scope")
                                .queryParam("client_id", TestConstants.VALID_CLIENT_ID)
                                .build()
                )
                .header("content-type", APPLICATION_FORM_URLENCODED_VALUE)
                .exchange()
                .expectStatus().is4xxClientError()
                .expectBody(ErrorResponse.class)
                .returnResult().getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.timestamp).isNotNull();
        assertThat(response).isEqualTo(
                new ErrorResponse(
                        response.timestamp,
                        400,
                        "Bad Request",
                        MSG_INVALID_REQUEST,
                        "/oauth/token"
                )
        );
    }

    @Test
    public void testCreateOauth2Token_Failure_NoClientId() {
        val response = restClient.post()
                .uri(builder ->
                        builder.path(OAUTH_PREFIX + "/token")
                                .queryParam("grant_type", password.name())
                                .queryParam("username", TestConstants.VALID_USERNAME)
                                .queryParam("password", TestConstants.VALID_PASSWORD)
                                .queryParam("scope", "some/scope")
                                .queryParam("client_secret", TestConstants.VALID_CLIENT_SECRET)
                                .build()
                )
                .header("content-type", APPLICATION_FORM_URLENCODED_VALUE)
                .exchange()
                .expectStatus().is4xxClientError()
                .expectBody(ErrorResponse.class)
                .returnResult().getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.timestamp).isNotNull();
        assertThat(response).isEqualTo(
                new ErrorResponse(
                        response.timestamp,
                        400,
                        "Bad Request",
                        MSG_INVALID_REQUEST,
                        "/oauth/token"
                )
        );
    }

    @Test
    public void testCreateOauth2Token_Failure_BadDomainPrefix() {
        val response = restClient.post()
                .uri("http://127.0.0.1:" + port + OAUTH_PREFIX + "/token?grant_type={}&username={}&password={}&scope={}",
                        password.name(), TestConstants.VALID_USERNAME, TestConstants.VALID_PASSWORD, "some/scope")
                .header("content-type", APPLICATION_FORM_URLENCODED_VALUE)
                .headers(h -> h.setBasicAuth(VALID_CLIENT_ID, VALID_CLIENT_SECRET))
                .exchange()
                .expectStatus().is4xxClientError()
                .expectBody(ErrorResponse.class)
                .returnResult().getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.timestamp).isNotNull();
        assertThat(response).isEqualTo(
                new ErrorResponse(
                        response.timestamp,
                        400,
                        "Bad Request",
                        "Domain prefix unknown: 127.0.0.1",
                        "/oauth/token"
                )
        );
    }
}
