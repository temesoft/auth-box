package com.authbox.web;

import com.google.common.base.Splitter;
import lombok.val;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

public class TestUtils {

    private TestUtils() {
    }

    /**
     * Authenticates a test account against the local server and retrieves the resulting session cookie.
     * <p>
     * This method performs a POST request to {@code /login} with form-encoded credentials.
     * It expects a {@link HttpStatus#FOUND} (302) response upon successful authentication,
     * as is typical with Spring Security's default redirect-based login.
     * </p>
     *
     * @param port the local server port to target
     * @return the raw {@code JSESSIONID} cookie string (e.g., "JSESSIONID=ABC123...")
     * @throws IllegalStateException if the response status is not 302 or if the 'Set-Cookie' header is missing
     */
    public static String authenticateAccountGetCookie(final int port) {
        val httpClient = java.net.http.HttpClient.newBuilder()
                .followRedirects(java.net.http.HttpClient.Redirect.NEVER)
                .build();
        val restClient = RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .build();

        val formData = new LinkedMultiValueMap<String, String>();
        formData.add("username", TestConstants.VALID_USERNAME);
        formData.add("password", TestConstants.VALID_PASSWORD);

        val authCall = restClient.post()
                .uri("http://localhost:" + port + "/login")
                .body(formData)
                .retrieve()
                .toEntity(String.class);

        if (authCall.getStatusCode() != HttpStatus.FOUND) {
            throw new IllegalStateException("Unable to authenticate with test credentials. Response status: " + authCall.getStatusCode());
        }

        val setCookieHeaders = authCall.getHeaders().get("Set-Cookie");
        if (setCookieHeaders == null || setCookieHeaders.isEmpty()) {
            throw new IllegalStateException("Authenticate request does not contain 'Set-Cookie' header");
        }

        return Splitter.on(";")
                .omitEmptyStrings()
                .trimResults()
                .splitToList(setCookieHeaders.getFirst())
                .getFirst();
    }
}
