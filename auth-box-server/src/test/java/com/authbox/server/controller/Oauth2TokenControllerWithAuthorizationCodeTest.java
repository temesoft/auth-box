package com.authbox.server.controller;

import com.authbox.base.model.ErrorResponse;
import com.authbox.base.model.OauthTokenResponse;
import com.authbox.server.Application;
import com.authbox.server.TestConstants;
import com.google.common.collect.ImmutableMap;
import jakarta.annotation.Nullable;
import lombok.val;
import org.apache.hc.core5.net.URLEncodedUtils;
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

import java.util.Map;

import static com.authbox.base.config.Constants.OAUTH_PREFIX;
import static com.authbox.base.model.GrantType.authorization_code;
import static com.authbox.base.model.GrantType.password;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.util.ObjectUtils.isEmpty;

@SpringBootTest(
        classes = {Application.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@TestPropertySource(locations = "classpath:application-test.properties")
public class Oauth2TokenControllerWithAuthorizationCodeTest {

    private static final String HEADER_SET_COOKIE = "Set-Cookie";

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void testCreateOauth2Token_Success_GetAuthorizePageBack() {
        val responseEntity = restTemplate.getForEntity(
                OAUTH_PREFIX + "/authorize"
                + "?client_id=" + TestConstants.VALID_CLIENT_ID
                + "&redirect_uri=" + TestConstants.VALID_REDIRECT_URL
                + "&response_type=code"
                + "&scope=another/scope some/scope",
                String.class
        );
        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            val response = responseEntity.getBody();
            assertThat(response)
                    .isNotEmpty()
                    .contains("<title>Authorize</title>")
                    .doesNotContain("text-danger"); // no errors
        } else {
            fail("Returned non 200");
        }
    }

    @Test
    public void testCreateOauth2Token_Success_PostAuthorize() {
        // First, send username & password
        val responseEntity = postPasswordAuthorization(null, String.class);
        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            val response = responseEntity.getBody();
            assertThat(response)
                    .isNotEmpty()
                    .contains("<title>Authorize scopes</title>")
                    .doesNotContain("text-danger"); // no errors
            val cookie = responseEntity.getHeaders().getFirst(HEADER_SET_COOKIE);

            // Second, scope selection
            val responseEntity2 = postScopeAuthorizationSelection(cookie, null);
            if (responseEntity2.getStatusCode().is3xxRedirection()) {
                val location = responseEntity2.getHeaders().getLocation();
                assertThat(location).isNotNull();
                assertThat(location.toString()).startsWith(TestConstants.VALID_REDIRECT_URL);
                val uriQuery = URLEncodedUtils.parse(location, UTF_8);
                assertThat(uriQuery).hasSize(2);
                assertThat(uriQuery.get(0).getName()).isEqualTo("code");
                assertThat(uriQuery.get(0).getValue()).hasSize(64);
                assertThat(uriQuery.get(1).getName()).isEqualTo("state");
                assertThat(uriQuery.get(1).getValue()).isEqualTo("1234567890");
                val authorizationCode = uriQuery.get(0).getValue();

                // Third, authorization code for access token exchange
                val responseEntity3 = postAuthorizationCodeTokenExchange(authorizationCode, ImmutableMap.of());
                if (responseEntity3.getStatusCode().is2xxSuccessful()) {
                    val response3 = responseEntity3.getBody();
                    assertThat(response3).isNotNull();
                    assertThat(response3.accessToken).isNotBlank();
                    assertThat(response3.tokenType).isEqualTo("bearer");
                    assertThat(response3.expiresIn).isEqualTo(3600);
                    assertThat(response3.refreshToken).isNotEmpty();
                    assertThat(response3.scope).contains("some/scope").contains("another/scope");
                } else {
                    fail("Returned non 200");
                }
            } else {
                fail("Returned non 200");
            }
        } else {
            fail("Returned non 200");
        }
    }

    @Test
    public void testCreateOauth2Token_Success_LessScope() {
        // First, send username & password
        val responseEntity = postPasswordAuthorization(ImmutableMap.of("scope", "some/scope"), String.class);
        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            val response = responseEntity.getBody();
            assertThat(response)
                    .isNotEmpty()
                    .contains("<title>Authorize scopes</title>")
                    .doesNotContain("text-danger"); // no errors
            val cookie = responseEntity.getHeaders().getFirst(HEADER_SET_COOKIE);

            // Second, scope selection
            val responseEntity2 = postScopeAuthorizationSelection(cookie, ImmutableMap.of("scope", "some/scope"));
            if (responseEntity2.getStatusCode().is3xxRedirection()) {
                val location = responseEntity2.getHeaders().getLocation();
                assertThat(location).isNotNull();
                assertThat(location.toString()).startsWith(TestConstants.VALID_REDIRECT_URL);
                val uriQuery = URLEncodedUtils.parse(location, UTF_8);
                assertThat(uriQuery).hasSize(2);
                assertThat(uriQuery.get(0).getName()).isEqualTo("code");
                assertThat(uriQuery.get(0).getValue()).hasSize(64);
                assertThat(uriQuery.get(1).getName()).isEqualTo("state");
                assertThat(uriQuery.get(1).getValue()).isEqualTo("1234567890");
                val authorizationCode = uriQuery.get(0).getValue();

                // Third, authorization code for access token exchange
                val responseEntity3 = postAuthorizationCodeTokenExchange(authorizationCode, ImmutableMap.of("scope", "some/scope"));
                if (responseEntity3.getStatusCode().is2xxSuccessful()) {
                    val response3 = responseEntity3.getBody();
                    assertThat(response3).isNotNull();
                    assertThat(response3.accessToken).isNotBlank();
                    assertThat(response3.tokenType).isEqualTo("bearer");
                    assertThat(response3.expiresIn).isEqualTo(3600);
                    assertThat(response3.refreshToken).isNotEmpty();
                    assertThat(response3.scope).isEqualTo("some/scope");
                } else {
                    fail("Returned non 200");
                }
            } else {
                fail("Returned non 200");
            }
        } else {
            fail("Returned non 200");
        }
    }

    @Test
    public void testCreateOauth2Token_Failure_NoScopeRequested() {
        // Send username & password without scope
        val responseEntity = postPasswordAuthorization(ImmutableMap.of("scope", ""), String.class);
        if (responseEntity.getStatusCode().is4xxClientError()) {
            assertThat(responseEntity).isNotNull();
        } else {
            fail("Returned non 4xx error");
        }
    }

    @Test
    public void testCreateOauth2Token_Failure_BadDomainPrefix() {
        val responseEntity = restTemplate.getForEntity(
                "http://127.0.0.1:" + port + OAUTH_PREFIX + "/authorize"
                + "?client_id=" + TestConstants.VALID_CLIENT_ID
                + "&redirect_uri=" + TestConstants.VALID_REDIRECT_URL
                + "&response_type=code"
                + "&scope=another/scope some/scope",
                ErrorResponse.class
        );
        if (responseEntity.getStatusCode().is4xxClientError()) {
            val response = responseEntity.getBody();
            assertThat(response).isNotNull();
            assertThat(response.timestamp).isNotNull();
            assertThat(response).isEqualTo(new ErrorResponse(response.timestamp, 400, "Bad Request", "Domain prefix unknown: 127.0.0.1", "/oauth/authorize"));
        } else {
            fail("Returned non 4xx error");
        }
    }

    private <T> ResponseEntity<T> postPasswordAuthorization(@Nullable final Map<String, String> paramsOverride, Class<T> clazz) {
        val headers = new HttpHeaders();
        headers.setContentType(APPLICATION_FORM_URLENCODED);
        val params = new LinkedMultiValueMap<String, String>();
        params.add("grant_type", password.name());
        params.add("username", TestConstants.VALID_USERNAME);
        params.add("password", TestConstants.VALID_PASSWORD);
        params.add("client_id", TestConstants.VALID_CLIENT_ID);
        params.add("redirect_uri", TestConstants.VALID_REDIRECT_URL);
        params.add("response_type", "code");
        params.add("scope", "some/scope another/scope");
        params.add("state", "1234567890");
        if (paramsOverride != null) {
            paramsOverride.keySet().forEach(key -> {
                params.remove(key);
                if (!isEmpty(paramsOverride.get(key))) {
                    params.add(key, paramsOverride.get(key));
                }
            });
        }
        val request = new HttpEntity<MultiValueMap<String, String>>(params, headers);
        return restTemplate.postForEntity(OAUTH_PREFIX + "/authorize", request, clazz);
    }

    private ResponseEntity<String> postScopeAuthorizationSelection(final String cookie, @Nullable final Map<String, String> paramsOverride) {
        val headers = new HttpHeaders();
        headers.setContentType(APPLICATION_FORM_URLENCODED);
        headers.set("Cookie", cookie);
        val params = new LinkedMultiValueMap<String, String>();
        params.add("grant_type", password.name());
        params.add("client_id", TestConstants.VALID_CLIENT_ID);
        params.add("redirect_uri", TestConstants.VALID_REDIRECT_URL);
        params.add("response_type", "code");
        params.add("scope", "some/scope another/scope");
        params.add("state", "1234567890");
        if (paramsOverride != null) {
            paramsOverride.keySet().forEach(key -> {
                params.remove(key);
                if (!isEmpty(paramsOverride.get(key))) {
                    params.add(key, paramsOverride.get(key));
                }
            });
        }
        val request = new HttpEntity<MultiValueMap<String, String>>(params, headers);
        return restTemplate.postForEntity(OAUTH_PREFIX + "/authorize/finish", request, String.class);
    }

    private ResponseEntity<OauthTokenResponse> postAuthorizationCodeTokenExchange(final String authorizationCode, @Nullable final Map<String, String> paramsOverride) {
        val headers = new HttpHeaders();
        headers.setContentType(APPLICATION_FORM_URLENCODED);
        val params = new LinkedMultiValueMap<String, String>();
        params.add("grant_type", authorization_code.name());
        params.add("code", authorizationCode);
        params.add("client_id", TestConstants.VALID_CLIENT_ID);
        params.add("client_secret", TestConstants.VALID_CLIENT_SECRET);
        if (paramsOverride != null) {
            paramsOverride.keySet().forEach(key -> {
                params.remove(key);
                if (!isEmpty(paramsOverride.get(key))) {
                    params.add(key, paramsOverride.get(key));
                }
            });
        }
        val request = new HttpEntity<MultiValueMap<String, String>>(params, headers);
        return restTemplate.postForEntity(OAUTH_PREFIX + "/token", request, OauthTokenResponse.class);
    }
}
