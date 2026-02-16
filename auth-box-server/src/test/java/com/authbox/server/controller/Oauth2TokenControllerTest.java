package com.authbox.server.controller;

import com.authbox.base.model.ErrorResponse;
import com.authbox.server.Application;
import com.authbox.server.TestConstants;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

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
import static com.authbox.server.TestConstants.VALID_CLIENT_ID;
import static com.authbox.server.TestConstants.VALID_CLIENT_SECRET;
import static com.authbox.server.TestConstants.VALID_TOKEN;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = {Application.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureRestTestClient
@TestPropertySource(locations = "classpath:application-test.properties")
public class Oauth2TokenControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private RestTestClient restClient;
    @Autowired
    private ObjectMapper objectMapper;

    private final ParameterizedTypeReference<Map<String, Object>> mapResponseType =
            new ParameterizedTypeReference<>() {
            };

    @Test
    public void testGetDetailsOauth2Token_Success_Parameter() {
        val responseMap = restClient.post()
                .uri(OAUTH_PREFIX + "/introspection?access_token=" + VALID_TOKEN + "&client_id=" + VALID_CLIENT_ID + "&client_secret=" + VALID_CLIENT_SECRET)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(mapResponseType)
                .returnResult().getResponseBody();
        val response = objectMapper.valueToTree(responseMap);
        assertThat(response).isNotNull();
        assertThat(response.get(OAUTH2_ATTR_ACTIVE).booleanValue()).isEqualTo(true);
        assertThat(response.get(OAUTH2_ATTR_EXPIRES_IN).longValue()).isGreaterThan(2057786133L);
        assertThat(response.get(OAUTH2_ATTR_EXPIRES).longValue()).isGreaterThan(2057786133L);
        assertThat(response.get(OAUTH2_ATTR_SCOPE).asString()).contains("some/scope", "another/scope");
        assertThat(response.get(OAUTH2_ATTR_CLIENT_ID).asString()).isEqualTo("5d94c101-0236-4a4d-b54b-dd8c446c384c");
        assertThat(response.get(OAUTH2_ATTR_ORGANIZATION_ID).asString()).isEqualTo("c1ade6b3-c023-44f4-b3ef-b0f27ba5e6e8");
        assertThat(response.get(OAUTH2_ATTR_USER_ID).asString()).isEqualTo("6c580763-c0c1-4f26-92c6-ffeba50dc4d5");
        assertThat(response.get(OAUTH2_ATTR_METADATA)).isNotNull();
    }

    @Test
    public void testGetDetailsOauth2Token_Success_ClientHeaders() {
        val responseMap = restClient.post()
                .uri(OAUTH_PREFIX + "/introspection?access_token=" + VALID_TOKEN)
                .headers(h -> h.setBasicAuth(VALID_CLIENT_ID, VALID_CLIENT_SECRET))
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(mapResponseType)
                .returnResult().getResponseBody();
        val response = objectMapper.valueToTree(responseMap);
        assertThat(response).isNotNull();
        assertThat(response.get(OAUTH2_ATTR_ACTIVE).booleanValue()).isEqualTo(true);
        assertThat(response.get(OAUTH2_ATTR_EXPIRES_IN).longValue()).isGreaterThan(2057786133L);
        assertThat(response.get(OAUTH2_ATTR_EXPIRES).longValue()).isGreaterThan(2057786133L);
        assertThat(response.get(OAUTH2_ATTR_SCOPE).asString()).contains("some/scope", "another/scope");
        assertThat(response.get(OAUTH2_ATTR_CLIENT_ID).asString()).isEqualTo("5d94c101-0236-4a4d-b54b-dd8c446c384c");
        assertThat(response.get(OAUTH2_ATTR_ORGANIZATION_ID).asString()).isEqualTo("c1ade6b3-c023-44f4-b3ef-b0f27ba5e6e8");
        assertThat(response.get(OAUTH2_ATTR_USER_ID).asString()).isEqualTo("6c580763-c0c1-4f26-92c6-ffeba50dc4d5");
        assertThat(response.get(OAUTH2_ATTR_METADATA)).isNotNull();
    }

    @Test
    public void testGetDetailsOauth2Token_Success_AuthHeader() {
        val responseMap = restClient.post()
                .uri(OAUTH_PREFIX + "/introspection?client_id=" + VALID_CLIENT_ID + "&client_secret=" + VALID_CLIENT_SECRET)
                .headers(h -> h.setBearerAuth(VALID_TOKEN))
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(mapResponseType)
                .returnResult().getResponseBody();
        val response = objectMapper.valueToTree(responseMap);
        assertThat(response).isNotNull();
        assertThat(response.get(OAUTH2_ATTR_ACTIVE).booleanValue()).isEqualTo(true);
        assertThat(response.get(OAUTH2_ATTR_EXPIRES_IN).longValue()).isGreaterThan(2057786133L);
        assertThat(response.get(OAUTH2_ATTR_EXPIRES).longValue()).isGreaterThan(2057786133L);
        assertThat(response.get(OAUTH2_ATTR_SCOPE).asString()).contains("some/scope", "another/scope");
        assertThat(response.get(OAUTH2_ATTR_CLIENT_ID).asString()).isEqualTo("5d94c101-0236-4a4d-b54b-dd8c446c384c");
        assertThat(response.get(OAUTH2_ATTR_ORGANIZATION_ID).asString()).isEqualTo("c1ade6b3-c023-44f4-b3ef-b0f27ba5e6e8");
        assertThat(response.get(OAUTH2_ATTR_USER_ID).asString()).isEqualTo("6c580763-c0c1-4f26-92c6-ffeba50dc4d5");
        assertThat(response.get(OAUTH2_ATTR_METADATA)).isNotNull();

    }

    @Test
    public void testGetDetailsOauth2Token_Expired() {
        val responseMap = restClient.post()
                .uri(OAUTH_PREFIX + "/introspection?access_token=" + TestConstants.EXPIRED_TOKEN + "&client_id=" + VALID_CLIENT_ID + "&client_secret=" + VALID_CLIENT_SECRET)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(mapResponseType)
                .returnResult().getResponseBody();
        val response = objectMapper.valueToTree(responseMap);
        assertThat(response).isNotNull();
        assertThat(response.get(OAUTH2_ATTR_ACTIVE).booleanValue()).isEqualTo(false);
        assertThat(response.get(OAUTH2_ATTR_EXPIRES_IN)).isNull();
        assertThat(response.get(OAUTH2_ATTR_EXPIRES)).isNull();
        assertThat(response.get(OAUTH2_ATTR_SCOPE)).isNull();
        assertThat(response.get(OAUTH2_ATTR_CLIENT_ID)).isNull();
        assertThat(response.get(OAUTH2_ATTR_ORGANIZATION_ID)).isNull();
        assertThat(response.get(OAUTH2_ATTR_USER_ID)).isNull();
        assertThat(response.get(OAUTH2_ATTR_METADATA)).isNull();
    }

    @Test
    public void testGetDetailsOauth2Token_BadToken() {
        val response = restClient.post()
                .uri(OAUTH_PREFIX + "/introspection?access_token=bad-token&client_id=" + VALID_CLIENT_ID + "&client_secret=" + VALID_CLIENT_SECRET)
                .exchange()
                .expectStatus().is4xxClientError()
                .expectBody(ErrorResponse.class)
                .returnResult().getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.timestamp).isNotNull();
        assertThat(response.error).isEqualTo("Unauthorized");
        assertThat(response.status).isEqualTo(401);
        assertThat(response.message).isEqualTo(MSG_UNAUTHORIZED_REQUEST);
        assertThat(response.path).isEqualTo("/oauth/introspection");
    }

    @Test
    public void testGetDetailsOauth2Token_NoToken() {
        val response = restClient.post()
                .uri(OAUTH_PREFIX + "/introspection?client_id=" + VALID_CLIENT_ID + "&client_secret=" + VALID_CLIENT_SECRET)
                .exchange()
                .expectStatus().is4xxClientError()
                .expectBody(ErrorResponse.class)
                .returnResult().getResponseBody();
        assertThat(response).isNotNull();
        assertThat(response.timestamp).isNotNull();
        assertThat(response.error).isEqualTo("Unauthorized");
        assertThat(response.status).isEqualTo(401);
        assertThat(response.message).isEqualTo(MSG_UNAUTHORIZED_REQUEST);
        assertThat(response.path).isEqualTo("/oauth/introspection");
    }

    @Test
    public void testGetDetailsOauth2Token_BadDomainPrefix() {
        val response = restClient.post()
                .uri("http://127.0.0.1:" + port + OAUTH_PREFIX + "/introspection?access_token=" + VALID_TOKEN
                        + "&client_id=" + VALID_CLIENT_ID + "&client_secret=" + VALID_CLIENT_SECRET)
                .exchange()
                .expectStatus().is4xxClientError()
                .expectBody(ErrorResponse.class)
                .returnResult().getResponseBody();
        assertThat(response).isNotNull();
        assertThat(response.timestamp).isNotNull();
        assertThat(response.error).isEqualTo("Bad Request");
        assertThat(response.status).isEqualTo(400);
        assertThat(response.message).isEqualTo("Domain prefix unknown: 127.0.0.1");
        assertThat(response.path).isEqualTo("/oauth/introspection");
    }
}