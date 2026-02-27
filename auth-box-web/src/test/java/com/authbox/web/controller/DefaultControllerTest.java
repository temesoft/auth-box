package com.authbox.web.controller;

import com.authbox.web.Application;
import com.authbox.web.TestUtils;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = {Application.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureRestTestClient
@TestPropertySource(locations = "classpath:application-test.properties")
class DefaultControllerTest {

    @LocalServerPort
    private int port;
    @Autowired
    private RestTestClient restTestClient;

    private String jSessionId;

    @BeforeEach
    public void setup() {
        jSessionId = TestUtils.authenticateAccountGetCookie(port);
    }

    @Test
    public void testIndexPage() {
        val htmlPage = restTestClient.get().uri("/")
                .exchange()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
        assertThat(htmlPage).isNotBlank()
                .contains("<title>AuthBox</title>");
    }

    @Test
    public void testRegisterPage() {
        val htmlPage = restTestClient.get().uri("/register.html")
                .exchange()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
        assertThat(htmlPage).isNotBlank()
                .contains("<title>AuthBox Registration</title>");
    }

    @Test
    public void testSignInPage() {
        val htmlPage = restTestClient.get().uri("/sign-in.html")
                .exchange()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
        assertThat(htmlPage).isNotBlank()
                .contains("<title>AuthBox Sign In</title>");
    }

    @Test
    public void testLoginPage() {
        val htmlPage = restTestClient.get().uri("/login")
                .exchange()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
        assertThat(htmlPage).isNotBlank()
                .contains("<title>AuthBox</title>");
    }

    @Test
    public void testSecureIndexPage() {
        val htmlPage = restTestClient.get().uri("/secure/")
                .header("cookie", jSessionId)
                .exchange()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
        assertThat(htmlPage).isNotBlank()
                .contains("<title>AuthBox Management Panel</title>");
    }

    @Test
    public void testSecurePage() {
        val htmlPage = restTestClient.get().uri("/secure/access-log.html")
                .header("cookie", jSessionId)
                .exchange()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
        assertThat(htmlPage).isNotBlank()
                .doesNotContain("<title>", "</title>")
                .contains("Request Log");
    }
}