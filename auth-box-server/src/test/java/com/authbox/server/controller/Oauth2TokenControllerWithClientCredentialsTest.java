package com.authbox.server.controller;

import com.authbox.base.model.ErrorResponse;
import com.authbox.base.model.OauthTokenResponse;
import com.authbox.server.Application;
import com.authbox.server.TestConstants;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static com.authbox.base.config.Constants.MSG_INVALID_REQUEST;
import static com.authbox.base.config.Constants.MSG_INVALID_SCOPE;
import static com.authbox.base.config.Constants.OAUTH_PREFIX;
import static com.authbox.base.model.GrantType.client_credentials;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;

@SpringBootTest(
        classes = {Application.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@TestPropertySource(locations = "classpath:application-test.properties")
public class Oauth2TokenControllerWithClientCredentialsTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void testCreateOauth2Token_Success_UsingAuthHeader() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(TestConstants.VALID_CLIENT_ID, TestConstants.VALID_CLIENT_SECRET);
        final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", client_credentials.name());
        params.add("scope", "some/scope another/scope");
        final HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        final ResponseEntity<OauthTokenResponse> responseEntity = restTemplate.postForEntity(OAUTH_PREFIX + "/token", request, OauthTokenResponse.class);
        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            final OauthTokenResponse response = responseEntity.getBody();
            assertThat(response).isNotNull();
            assertThat(response.accessToken).isNotBlank();
            assertThat(response.tokenType).isEqualTo("bearer");
            assertThat(response.expiresIn).isEqualTo(3600);
            assertThat(response.refreshToken).isNull();
            assertThat(response.scope).contains("some/scope", "another/scope");
        } else {
            fail("Returned non 200");
        }
    }

    @Test
    public void testCreateOauth2Token_Success_UsingParams() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_FORM_URLENCODED);
        final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", client_credentials.name());
        params.add("scope", "some/scope another/scope");
        params.add("client_id", TestConstants.VALID_CLIENT_ID);
        params.add("client_secret", TestConstants.VALID_CLIENT_SECRET);
        final HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        final ResponseEntity<OauthTokenResponse> responseEntity = restTemplate.postForEntity(OAUTH_PREFIX + "/token", request, OauthTokenResponse.class);
        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            final OauthTokenResponse response = responseEntity.getBody();
            assertThat(response).isNotNull();
            assertThat(response.accessToken).isNotBlank();
            assertThat(response.tokenType).isEqualTo("bearer");
            assertThat(response.expiresIn).isEqualTo(3600);
            assertThat(response.refreshToken).isNull();
            assertThat(response.scope).contains("some/scope").contains("another/scope");
        } else {
            fail("Returned non 200");
        }
    }

    @Test
    public void testCreateOauth2Token_Success_LessScope() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_FORM_URLENCODED);
        final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", client_credentials.name());
        params.add("scope", "some/scope");
        params.add("client_id", TestConstants.VALID_CLIENT_ID);
        params.add("client_secret", TestConstants.VALID_CLIENT_SECRET);
        final HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        final ResponseEntity<OauthTokenResponse> responseEntity = restTemplate.postForEntity(OAUTH_PREFIX + "/token", request, OauthTokenResponse.class);
        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            final OauthTokenResponse response = responseEntity.getBody();
            assertThat(response).isNotNull();
            assertThat(response.accessToken).isNotBlank();
            assertThat(response.tokenType).isEqualTo("bearer");
            assertThat(response.expiresIn).isEqualTo(3600);
            assertThat(response.refreshToken).isNull();
            assertThat(response.scope).isEqualTo("some/scope");
        } else {
            fail("Returned non 200");
        }
    }

    @Test
    public void testCreateOauth2Token_Success_NoScopeSpecified() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_FORM_URLENCODED);
        final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", client_credentials.name());
        params.add("client_id", TestConstants.VALID_CLIENT_ID);
        params.add("client_secret", TestConstants.VALID_CLIENT_SECRET);
        final HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        final ResponseEntity<OauthTokenResponse> responseEntity = restTemplate.postForEntity(OAUTH_PREFIX + "/token", request, OauthTokenResponse.class);
        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            final OauthTokenResponse response = responseEntity.getBody();
            assertThat(response).isNotNull();
            assertThat(response.accessToken).isNotBlank();
            assertThat(response.tokenType).isEqualTo("bearer");
            assertThat(response.expiresIn).isEqualTo(3600);
            assertThat(response.refreshToken).isNull();
            assertThat(response.scope).isNull();
        } else {
            fail("Returned non 200");
        }
    }

    @Test
    public void testCreateOauth2Token_Failure_BadScope() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_FORM_URLENCODED);
        final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", client_credentials.name());
        params.add("scope", "scope/NotOnAllowList");
        params.add("client_id", TestConstants.VALID_CLIENT_ID);
        params.add("client_secret", TestConstants.VALID_CLIENT_SECRET);
        final HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        final ResponseEntity<ErrorResponse> responseEntity = restTemplate.postForEntity(OAUTH_PREFIX + "/token", request, ErrorResponse.class);
        if (responseEntity.getStatusCode().is4xxClientError()) {
            final ErrorResponse response = responseEntity.getBody();
            assertThat(response).isNotNull();
            assertThat(response.timestamp).isNotNull();
            assertThat(response).isEqualTo(new ErrorResponse(response.timestamp, 400, "Bad Request", MSG_INVALID_SCOPE, "/oauth/token"));
        } else {
            fail("Returned non 4xx error");
        }
    }

    @Test
    public void testCreateOauth2Token_Failure_BadClientId() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_FORM_URLENCODED);
        final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", client_credentials.name());
        params.add("scope", "some/scope");
        params.add("client_id", "bad-client-id");
        params.add("client_secret", TestConstants.VALID_CLIENT_SECRET);
        final HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        final ResponseEntity<ErrorResponse> responseEntity = restTemplate.postForEntity(OAUTH_PREFIX + "/token", request, ErrorResponse.class);
        if (responseEntity.getStatusCode().is4xxClientError()) {
            final ErrorResponse response = responseEntity.getBody();
            assertThat(response).isNotNull();
            assertThat(response.timestamp).isNotNull();
            assertThat(response).isEqualTo(new ErrorResponse(response.timestamp, 400, "Bad Request", MSG_INVALID_REQUEST, "/oauth/token"));
        } else {
            fail("Returned non 4xx error");
        }
    }

    @Test
    public void testCreateOauth2Token_Failure_BadClientSecret() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_FORM_URLENCODED);
        final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", client_credentials.name());
        params.add("scope", "some/scope");
        params.add("client_id", TestConstants.VALID_CLIENT_ID);
        params.add("client_secret", "this-is-a-wrong-secret");
        final HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        final ResponseEntity<ErrorResponse> responseEntity = restTemplate.postForEntity(OAUTH_PREFIX + "/token", request, ErrorResponse.class);
        if (responseEntity.getStatusCode().is4xxClientError()) {
            final ErrorResponse response = responseEntity.getBody();
            assertThat(response).isNotNull();
            assertThat(response.timestamp).isNotNull();
            assertThat(response).isEqualTo(new ErrorResponse(response.timestamp, 400, "Bad Request", MSG_INVALID_REQUEST, "/oauth/token"));
        } else {
            fail("Returned non 4xx error");
        }
    }

    @Test
    public void testCreateOauth2Token_Failure_NoClientSecret() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_FORM_URLENCODED);
        final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", client_credentials.name());
        params.add("scope", "some/scope");
        params.add("client_id", TestConstants.VALID_CLIENT_ID);
        final HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        final ResponseEntity<ErrorResponse> responseEntity = restTemplate.postForEntity(OAUTH_PREFIX + "/token", request, ErrorResponse.class);
        if (responseEntity.getStatusCode().is4xxClientError()) {
            final ErrorResponse response = responseEntity.getBody();
            assertThat(response).isNotNull();
            assertThat(response.timestamp).isNotNull();
            assertThat(response).isEqualTo(new ErrorResponse(response.timestamp, 400, "Bad Request", MSG_INVALID_REQUEST, "/oauth/token"));
        } else {
            fail("Returned non 4xx error");
        }
    }

    @Test
    public void testCreateOauth2Token_Failure_NoClientId() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_FORM_URLENCODED);
        final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", client_credentials.name());
        params.add("scope", "some/scope");
        params.add("client_secret", TestConstants.VALID_CLIENT_SECRET);
        final HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        final ResponseEntity<ErrorResponse> responseEntity = restTemplate.postForEntity(OAUTH_PREFIX + "/token", request, ErrorResponse.class);
        if (responseEntity.getStatusCode().is4xxClientError()) {
            final ErrorResponse response = responseEntity.getBody();
            assertThat(response).isNotNull();
            assertThat(response.timestamp).isNotNull();
            assertThat(response).isEqualTo(new ErrorResponse(response.timestamp, 400, "Bad Request", MSG_INVALID_REQUEST, "/oauth/token"));
        } else {
            fail("Returned non 4xx error");
        }
    }

    @Test
    public void testCreateOauth2Token_Failure_BadDomainPrefix() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(TestConstants.VALID_CLIENT_ID, TestConstants.VALID_CLIENT_SECRET);
        final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", client_credentials.name());
        params.add("scope", "some/scope another/scope");
        final HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        final ResponseEntity<ErrorResponse> responseEntity = restTemplate.postForEntity(
                "http://127.0.0.1:" + port + OAUTH_PREFIX + "/token", request, ErrorResponse.class);
        if (responseEntity.getStatusCode().is4xxClientError()) {
            final ErrorResponse response = responseEntity.getBody();
            assertThat(response).isNotNull();
            assertThat(response.timestamp).isNotNull();
            assertThat(response).isEqualTo(new ErrorResponse(response.timestamp, 400, "Bad Request", "Domain prefix unknown: 127.0.0.1", "/oauth/token"));
        } else {
            fail("Returned non 4xx error");
        }
    }
}
