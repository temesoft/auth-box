package com.authbox.server.controller;

import com.authbox.base.model.ErrorResponse;
import com.authbox.server.Application;
import com.authbox.server.TestConstants;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static com.authbox.base.config.Constants.MSG_UNAUTHORIZED_REQUEST;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_ACTIVE;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_CLIENT_ID;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_EXPIRES;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_EXPIRES_IN;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_METADATA;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_ORGANIZATION_ID;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_SCOPE;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_USER_ID;
import static com.authbox.base.config.Constants.OAUTH_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@SpringBootTest(
        classes = {Application.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@TestPropertySource(locations = "classpath:application-test.properties")
public class Oauth2TokenControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void testGetDetailsOauth2Token_Success_Parameter() {
        final ResponseEntity<JsonNode> responseEntity = restTemplate.postForEntity(
                OAUTH_PREFIX + "/introspection?access_token=" + TestConstants.VALID_TOKEN + "&client_id=" + TestConstants.VALID_CLIENT_ID + "&client_secret=" + TestConstants.VALID_CLIENT_SECRET,
                "",
                JsonNode.class);
        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            final JsonNode response = responseEntity.getBody();
            assertThat(response).isNotNull();
            assertThat(response.get(OAUTH2_ATTR_ACTIVE).booleanValue()).isEqualTo(true);
            assertThat(response.get(OAUTH2_ATTR_EXPIRES_IN).longValue()).isGreaterThan(2057786133L);
            assertThat(response.get(OAUTH2_ATTR_EXPIRES).longValue()).isGreaterThan(2057786133L);
            assertThat(response.get(OAUTH2_ATTR_SCOPE).textValue()).contains("some/scope", "another/scope");
            assertThat(response.get(OAUTH2_ATTR_CLIENT_ID).textValue()).isEqualTo("5d94c101-0236-4a4d-b54b-dd8c446c384c");
            assertThat(response.get(OAUTH2_ATTR_ORGANIZATION_ID).textValue()).isEqualTo("c1ade6b3-c023-44f4-b3ef-b0f27ba5e6e8");
            assertThat(response.get(OAUTH2_ATTR_USER_ID).textValue()).isEqualTo("6c580763-c0c1-4f26-92c6-ffeba50dc4d5");
            assertThat(response.get(OAUTH2_ATTR_METADATA)).isNotNull();
        } else {
            fail("Returned non 200");
        }
    }

    @Test
    public void testGetDetailsOauth2Token_Success_ClientHeaders() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(TestConstants.VALID_CLIENT_ID, TestConstants.VALID_CLIENT_SECRET);
        final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        final HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        final ResponseEntity<JsonNode> responseEntity = restTemplate.exchange(
                OAUTH_PREFIX + "/introspection?access_token=" + TestConstants.VALID_TOKEN,
                HttpMethod.POST,
                request,
                JsonNode.class);
        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            final JsonNode response = responseEntity.getBody();
            assertThat(response).isNotNull();
            assertThat(response.get(OAUTH2_ATTR_ACTIVE).booleanValue()).isEqualTo(true);
            assertThat(response.get(OAUTH2_ATTR_EXPIRES_IN).longValue()).isGreaterThan(2057786133L);
            assertThat(response.get(OAUTH2_ATTR_EXPIRES).longValue()).isGreaterThan(2057786133L);
            assertThat(response.get(OAUTH2_ATTR_SCOPE).textValue()).contains("some/scope", "another/scope");
            assertThat(response.get(OAUTH2_ATTR_CLIENT_ID).textValue()).isEqualTo("5d94c101-0236-4a4d-b54b-dd8c446c384c");
            assertThat(response.get(OAUTH2_ATTR_ORGANIZATION_ID).textValue()).isEqualTo("c1ade6b3-c023-44f4-b3ef-b0f27ba5e6e8");
            assertThat(response.get(OAUTH2_ATTR_USER_ID).textValue()).isEqualTo("6c580763-c0c1-4f26-92c6-ffeba50dc4d5");
            assertThat(response.get(OAUTH2_ATTR_METADATA)).isNotNull();
        } else {
            fail("Returned non 200");
        }
    }

    @Test
    public void testGetDetailsOauth2Token_Success_AuthHeader() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TestConstants.VALID_TOKEN);
        final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        final HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        final ResponseEntity<JsonNode> responseEntity = restTemplate.exchange(
                OAUTH_PREFIX + "/introspection?client_id=" + TestConstants.VALID_CLIENT_ID + "&client_secret=" + TestConstants.VALID_CLIENT_SECRET,
                HttpMethod.POST,
                request,
                JsonNode.class);
        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            final JsonNode response = responseEntity.getBody();
            assertThat(response).isNotNull();
            assertThat(response.get(OAUTH2_ATTR_ACTIVE).booleanValue()).isEqualTo(true);
            assertThat(response.get(OAUTH2_ATTR_EXPIRES_IN).longValue()).isGreaterThan(2057786133L);
            assertThat(response.get(OAUTH2_ATTR_EXPIRES).longValue()).isGreaterThan(2057786133L);
            assertThat(response.get(OAUTH2_ATTR_SCOPE).textValue()).contains("some/scope", "another/scope");
            assertThat(response.get(OAUTH2_ATTR_CLIENT_ID).textValue()).isEqualTo("5d94c101-0236-4a4d-b54b-dd8c446c384c");
            assertThat(response.get(OAUTH2_ATTR_ORGANIZATION_ID).textValue()).isEqualTo("c1ade6b3-c023-44f4-b3ef-b0f27ba5e6e8");
            assertThat(response.get(OAUTH2_ATTR_USER_ID).textValue()).isEqualTo("6c580763-c0c1-4f26-92c6-ffeba50dc4d5");
            assertThat(response.get(OAUTH2_ATTR_METADATA)).isNotNull();
        } else {
            fail("Returned non 200");
        }
    }

    @Test
    public void testGetDetailsOauth2Token_Expired() {
        final ResponseEntity<JsonNode> responseEntity = restTemplate.postForEntity(
                OAUTH_PREFIX + "/introspection?access_token=" + TestConstants.EXPIRED_TOKEN + "&client_id=" + TestConstants.VALID_CLIENT_ID + "&client_secret=" + TestConstants.VALID_CLIENT_SECRET,
                "",
                JsonNode.class);
        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            final JsonNode response = responseEntity.getBody();
            assertThat(response).isNotNull();
            assertThat(response.get(OAUTH2_ATTR_ACTIVE).booleanValue()).isEqualTo(false);
            assertThat(response.get(OAUTH2_ATTR_EXPIRES_IN)).isNull();
            assertThat(response.get(OAUTH2_ATTR_EXPIRES)).isNull();
            assertThat(response.get(OAUTH2_ATTR_SCOPE)).isNull();
            assertThat(response.get(OAUTH2_ATTR_CLIENT_ID)).isNull();
            assertThat(response.get(OAUTH2_ATTR_ORGANIZATION_ID)).isNull();
            assertThat(response.get(OAUTH2_ATTR_USER_ID)).isNull();
            assertThat(response.get(OAUTH2_ATTR_METADATA)).isNull();
        } else {
            fail("Returned non 200");
        }
    }

    @Test
    public void testGetDetailsOauth2Token_BadToken() {
        final ResponseEntity<ErrorResponse> responseEntity = restTemplate.postForEntity(
                OAUTH_PREFIX + "/introspection?access_token=bad-token&client_id=" + TestConstants.VALID_CLIENT_ID + "&client_secret=" + TestConstants.VALID_CLIENT_SECRET,
                "",
                ErrorResponse.class);
        if (responseEntity.getStatusCode().is4xxClientError()) {
            final ErrorResponse response = responseEntity.getBody();
            assertThat(response).isNotNull();
            assertThat(response.timestamp).isNotNull();
            assertThat(response.error).isEqualTo("Unauthorized");
            assertThat(response.status).isEqualTo(401);
            assertThat(response.message).isEqualTo(MSG_UNAUTHORIZED_REQUEST);
            assertThat(response.path).isEqualTo("/oauth/introspection");
        } else {
            fail("Returned non 4xx error");
        }
    }

    @Test
    public void testGetDetailsOauth2Token_NoToken() {
        final ResponseEntity<ErrorResponse> responseEntity = restTemplate.postForEntity(
                OAUTH_PREFIX + "/introspection?client_id=" + TestConstants.VALID_CLIENT_ID + "&client_secret=" + TestConstants.VALID_CLIENT_SECRET,
                "",
                ErrorResponse.class);
        if (responseEntity.getStatusCode().is4xxClientError()) {
            final ErrorResponse response = responseEntity.getBody();
            assertThat(response).isNotNull();
            assertThat(response.timestamp).isNotNull();
            assertThat(response.error).isEqualTo("Unauthorized");
            assertThat(response.status).isEqualTo(401);
            assertThat(response.message).isEqualTo(MSG_UNAUTHORIZED_REQUEST);
            assertThat(response.path).isEqualTo("/oauth/introspection");
        } else {
            fail("Returned non 4xx error");
        }
    }

    @Test
    public void testGetDetailsOauth2Token_BadDomainPrefix() {
        final ResponseEntity<ErrorResponse> responseEntity = restTemplate.postForEntity(
                "http://127.0.0.1:" + port + OAUTH_PREFIX + "/introspection?access_token=" + TestConstants.VALID_TOKEN
                + "&client_id=" + TestConstants.VALID_CLIENT_ID + "&client_secret=" + TestConstants.VALID_CLIENT_SECRET,
                "",
                ErrorResponse.class);
        if (responseEntity.getStatusCode().is4xxClientError()) {
            final ErrorResponse response = responseEntity.getBody();
            assertThat(response).isNotNull();
            assertThat(response.timestamp).isNotNull();
            assertThat(response.error).isEqualTo("Bad Request");
            assertThat(response.status).isEqualTo(400);
            assertThat(response.message).isEqualTo("Domain prefix unknown: 127.0.0.1");
            assertThat(response.path).isEqualTo("/oauth/introspection");
        } else {
            fail("Returned non 4xx error");
        }
    }
}