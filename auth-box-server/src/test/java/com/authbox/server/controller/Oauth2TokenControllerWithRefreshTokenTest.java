package com.authbox.server.controller;

import com.authbox.base.model.ErrorResponse;
import com.authbox.base.model.OauthTokenResponse;
import com.authbox.server.Application;
import com.authbox.server.TestConstants;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static com.authbox.base.config.Constants.MSG_INVALID_REQUEST;
import static com.authbox.base.config.Constants.MSG_INVALID_TOKEN;
import static com.authbox.base.config.Constants.OAUTH_PREFIX;
import static com.authbox.base.model.GrantType.refresh_token;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;

@SpringBootTest(
        classes = {Application.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@TestPropertySource(locations = "classpath:application-test.properties")
public class Oauth2TokenControllerWithRefreshTokenTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void testCreateOauth2Token_Success_UsingParameter() {
        val headers = new HttpHeaders();
        headers.setContentType(APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(TestConstants.VALID_CLIENT_ID, TestConstants.VALID_CLIENT_SECRET);
        val params = new LinkedMultiValueMap<String, String>();
        params.add("grant_type", refresh_token.name());
        params.add("refresh_token", TestConstants.VALID_REFRESH_TOKEN);
        params.add("scope", "some/scope another/scope");
        val request = new HttpEntity<MultiValueMap<String, String>>(params, headers);
        val responseEntity = restTemplate.postForEntity(OAUTH_PREFIX + "/token", request, OauthTokenResponse.class);
        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            val response = responseEntity.getBody();
            assertThat(response).isNotNull();
            assertThat(response.accessToken).isNotBlank();
            assertThat(response.tokenType).isEqualTo("bearer");
            assertThat(response.expiresIn).isEqualTo(3600);
            assertThat(response.refreshToken).isNull();
            assertThat(response.scope).isEqualTo("some/scope another/scope");
        } else {
            fail("Returned non 200");
        }
    }

    @Test
    public void testCreateOauth2Token_Success_LessScope() {
        val headers = new HttpHeaders();
        headers.setContentType(APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(TestConstants.VALID_CLIENT_ID, TestConstants.VALID_CLIENT_SECRET);
        val params = new LinkedMultiValueMap<String, String>();
        params.add("grant_type", refresh_token.name());
        params.add("refresh_token", TestConstants.VALID_REFRESH_TOKEN);
        params.add("scope", "some/scope");
        val request = new HttpEntity<MultiValueMap<String, String>>(params, headers);
        val responseEntity = restTemplate.postForEntity(OAUTH_PREFIX + "/token", request, OauthTokenResponse.class);
        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            val response = responseEntity.getBody();
            assertThat(response).isNotNull();
            assertThat(response.accessToken).isNotBlank();
            assertThat(response.tokenType).isEqualTo("bearer");
            assertThat(response.expiresIn).isEqualTo(3600);
            assertThat(response.refreshToken).isNull();
            // 2 scopes are still be based on original access token request
            assertThat(response.scope).isEqualTo("some/scope another/scope");
        } else {
            fail("Returned non 200");
        }
    }

    @Test
    public void testCreateOauth2Token_Success_NoScope() {
        val headers = new HttpHeaders();
        headers.setContentType(APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(TestConstants.VALID_CLIENT_ID, TestConstants.VALID_CLIENT_SECRET);
        val params = new LinkedMultiValueMap<String, String>();
        params.add("grant_type", refresh_token.name());
        params.add("refresh_token", TestConstants.VALID_REFRESH_TOKEN);
        val request = new HttpEntity<MultiValueMap<String, String>>(params, headers);
        val responseEntity = restTemplate.postForEntity(OAUTH_PREFIX + "/token", request, OauthTokenResponse.class);
        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            val response = responseEntity.getBody();
            assertThat(response).isNotNull();
            assertThat(response.accessToken).isNotBlank();
            assertThat(response.tokenType).isEqualTo("bearer");
            assertThat(response.expiresIn).isEqualTo(3600);
            assertThat(response.refreshToken).isNull();
            // 2 scopes are still be based on original access token request
            assertThat(response.scope).isEqualTo("some/scope another/scope");
        } else {
            fail("Returned non 200");
        }
    }

    @Test
    public void testCreateOauth2Token_Failure_UsingExpiredRefreshToken() {
        val headers = new HttpHeaders();
        headers.setContentType(APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(TestConstants.VALID_CLIENT_ID, TestConstants.VALID_CLIENT_SECRET);
        val params = new LinkedMultiValueMap<String, String>();
        params.add("grant_type", refresh_token.name());
        params.add("refresh_token", TestConstants.EXPIRED_REFRESH_TOKEN);
        params.add("scope", "some/scope another/scope");
        val request = new HttpEntity<MultiValueMap<String, String>>(params, headers);
        val responseEntity = restTemplate.postForEntity(OAUTH_PREFIX + "/token", request, ErrorResponse.class);
        if (responseEntity.getStatusCode().is4xxClientError()) {
            val response = responseEntity.getBody();
            assertThat(response).isNotNull();
            assertThat(response.timestamp).isNotNull();
            assertThat(response).isEqualTo(new ErrorResponse(response.timestamp, 401, "Unauthorized", MSG_INVALID_TOKEN, "/oauth/token"));
        } else {
            fail("Returned non 4xx error");
        }
    }

    @Test
    public void testCreateOauth2Token_Failure_UsingWrongTokenType() {
        val headers = new HttpHeaders();
        headers.setContentType(APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(TestConstants.VALID_CLIENT_ID, TestConstants.VALID_CLIENT_SECRET);
        val params = new LinkedMultiValueMap<String, String>();
        params.add("grant_type", refresh_token.name());
        params.add("refresh_token", TestConstants.VALID_TOKEN); // this token is ACCESS_TOKEN and not REFRESH_TOKEN
        params.add("scope", "some/scope another/scope");
        val request = new HttpEntity<MultiValueMap<String, String>>(params, headers);
        val responseEntity = restTemplate.postForEntity(OAUTH_PREFIX + "/token", request, ErrorResponse.class);
        if (responseEntity.getStatusCode().is4xxClientError()) {
            val response = responseEntity.getBody();
            assertThat(response).isNotNull();
            assertThat(response.timestamp).isNotNull();
            assertThat(response).isEqualTo(new ErrorResponse(response.timestamp, 401, "Unauthorized", MSG_INVALID_TOKEN, "/oauth/token"));
        } else {
            fail("Returned non 4xx error");
        }
    }

    @Test
    public void testCreateOauth2Token_Failure_BadClientId() {
        val headers = new HttpHeaders();
        headers.setContentType(APPLICATION_FORM_URLENCODED);
        val params = new LinkedMultiValueMap<String, String>();
        params.add("grant_type", refresh_token.name());
        params.add("refresh_token", TestConstants.VALID_REFRESH_TOKEN);
        params.add("scope", "some/scope");
        params.add("client_id", "bad-client-id");
        params.add("client_secret", TestConstants.VALID_CLIENT_SECRET);
        val request = new HttpEntity<MultiValueMap<String, String>>(params, headers);
        val responseEntity = restTemplate.postForEntity(OAUTH_PREFIX + "/token", request, ErrorResponse.class);
        if (responseEntity.getStatusCode().is4xxClientError()) {
            val response = responseEntity.getBody();
            assertThat(response).isNotNull();
            assertThat(response.timestamp).isNotNull();
            assertThat(response).isEqualTo(new ErrorResponse(response.timestamp, 400, "Bad Request", MSG_INVALID_REQUEST, "/oauth/token"));
        } else {
            fail("Returned non 4xx error");
        }
    }

    @Test
    public void testCreateOauth2Token_Failure_BadClientSecret() {
        val headers = new HttpHeaders();
        headers.setContentType(APPLICATION_FORM_URLENCODED);
        val params = new LinkedMultiValueMap<String, String>();
        params.add("grant_type", refresh_token.name());
        params.add("refresh_token", TestConstants.VALID_REFRESH_TOKEN);
        params.add("scope", "some/scope");
        params.add("client_id", TestConstants.VALID_CLIENT_ID);
        params.add("client_secret", "this-is-a-wrong-secret");
        val request = new HttpEntity<MultiValueMap<String, String>>(params, headers);
        val responseEntity = restTemplate.postForEntity(OAUTH_PREFIX + "/token", request, ErrorResponse.class);
        if (responseEntity.getStatusCode().is4xxClientError()) {
            val response = responseEntity.getBody();
            assertThat(response).isNotNull();
            assertThat(response.timestamp).isNotNull();
            assertThat(response).isEqualTo(new ErrorResponse(response.timestamp, 400, "Bad Request", MSG_INVALID_REQUEST, "/oauth/token"));
        } else {
            fail("Returned non 4xx error");
        }
    }

    @Test
    public void testCreateOauth2Token_Failure_NoClientSecret() {
        val headers = new HttpHeaders();
        headers.setContentType(APPLICATION_FORM_URLENCODED);
        val params = new LinkedMultiValueMap<String, String>();
        params.add("grant_type", refresh_token.name());
        params.add("refresh_token", TestConstants.VALID_REFRESH_TOKEN);
        params.add("scope", "some/scope");
        params.add("client_id", TestConstants.VALID_CLIENT_ID);
        val request = new HttpEntity<MultiValueMap<String, String>>(params, headers);
        val responseEntity = restTemplate.postForEntity(OAUTH_PREFIX + "/token", request, ErrorResponse.class);
        if (responseEntity.getStatusCode().is4xxClientError()) {
            val response = responseEntity.getBody();
            assertThat(response).isNotNull();
            assertThat(response.timestamp).isNotNull();
            assertThat(response).isEqualTo(new ErrorResponse(response.timestamp, 400, "Bad Request", MSG_INVALID_REQUEST, "/oauth/token"));
        } else {
            fail("Returned non 4xx error");
        }
    }

    @Test
    public void testCreateOauth2Token_Failure_NoClientId() {
        val headers = new HttpHeaders();
        headers.setContentType(APPLICATION_FORM_URLENCODED);
        val params = new LinkedMultiValueMap<String, String>();
        params.add("grant_type", refresh_token.name());
        params.add("refresh_token", TestConstants.VALID_REFRESH_TOKEN);
        params.add("scope", "some/scope");
        params.add("client_secret", TestConstants.VALID_CLIENT_SECRET);
        val request = new HttpEntity<MultiValueMap<String, String>>(params, headers);
        val responseEntity = restTemplate.postForEntity(OAUTH_PREFIX + "/token", request, ErrorResponse.class);
        if (responseEntity.getStatusCode().is4xxClientError()) {
            val response = responseEntity.getBody();
            assertThat(response).isNotNull();
            assertThat(response.timestamp).isNotNull();
            assertThat(response).isEqualTo(new ErrorResponse(response.timestamp, 400, "Bad Request", MSG_INVALID_REQUEST, "/oauth/token"));
        } else {
            fail("Returned non 4xx error");
        }
    }

    @Test
    public void testCreateOauth2Token_Failure_BadDomainPrefix() {
        val headers = new HttpHeaders();
        headers.setContentType(APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(TestConstants.VALID_CLIENT_ID, TestConstants.VALID_CLIENT_SECRET);
        val params = new LinkedMultiValueMap<String, String>();
        params.add("grant_type", refresh_token.name());
        params.add("refresh_token", TestConstants.VALID_REFRESH_TOKEN);
        params.add("scope", "some/scope another/scope");
        val request = new HttpEntity<MultiValueMap<String, String>>(params, headers);
        val responseEntity = restTemplate.postForEntity(
                "http://127.0.0.1:" + port + OAUTH_PREFIX + "/token", request, ErrorResponse.class);
        if (responseEntity.getStatusCode().is4xxClientError()) {
            val response = responseEntity.getBody();
            assertThat(response).isNotNull();
            assertThat(response.timestamp).isNotNull();
            assertThat(response).isEqualTo(new ErrorResponse(response.timestamp, 400, "Bad Request", "Domain prefix unknown: 127.0.0.1", "/oauth/token"));
        } else {
            fail("Returned non 4xx error");
        }
    }
}
