package com.authbox.web.controller;

import com.authbox.base.dao.OauthUserRepository;
import com.authbox.base.model.OauthUserRequest;
import com.authbox.web.Application;
import com.authbox.web.TestUtils;
import com.authbox.web.model.DeleteUsersRequest;
import com.authbox.web.model.PasswordChangeRequest;
import com.authbox.web.service.Oauth2UsersService;
import lombok.val;
import org.apache.commons.lang3.RandomStringUtils;
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

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static com.authbox.base.util.IdUtils.createId;
import static com.authbox.web.TestConstants.VALID_OAUTH_USER_ID;
import static com.authbox.web.TestConstants.VALID_ORGANIZATION_ID;
import static com.authbox.web.config.Constants.API_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = {Application.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureRestTestClient
@TestPropertySource(locations = "classpath:application-test.properties")
class Oauth2UsersControllerTest {

    @LocalServerPort
    private int port;
    @Autowired
    private RestTestClient restTestClient;
    @Autowired
    private OauthUserRepository oauthUserRepository;

    private String jSessionId;

    @BeforeEach
    public void setup() {
        jSessionId = TestUtils.authenticateAccountGetCookie(port);
    }

    @Test
    void testGetOauth2Users() {
        val pageOfUsers = restTestClient.get()
                .uri(API_PREFIX + "/oauth2-user")
                .header("cookie", jSessionId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .returnResult()
                .getResponseBody();
        assertThat(pageOfUsers)
                .isNotNull()
                .containsKey("content")
                .containsKey("page");
    }

    @Test
    void testGetOauth2UserById() {
        val badId = createId();
        assertThat(
                restTestClient.get()
                        .uri(API_PREFIX + "/oauth2-user/" + badId)
                        .header("cookie", jSessionId)
                        .exchange()
                        .expectStatus().isNotFound()
                        .expectBody(String.class)
                        .returnResult().getResponseBody()
        ).contains("User not found by id: " + badId);

        val user = restTestClient.get()
                .uri(API_PREFIX + "/oauth2-user/" + VALID_OAUTH_USER_ID)
                .header("cookie", jSessionId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Oauth2UsersService.OauthUserDto.class)
                .returnResult()
                .getResponseBody();
        assertThat(user).isNotNull();
        assertThat(user.getUsername()).isEqualTo("user1");
        assertThat(user.getOrganizationId()).isEqualTo(VALID_ORGANIZATION_ID);
        assertThat(user.getMetadata()).isEqualTo("{}");
    }

    @Test
    void testUpdatePassword() {
        var passwordChangeRequest = new PasswordChangeRequest(
                "password",
                "password2",
                "password3"
        );

        val badId = createId();
        assertThat(
                restTestClient.post()
                        .uri(API_PREFIX + "/oauth2-user/" + badId + "/password-reset")
                        .header("cookie", jSessionId)
                        .body(passwordChangeRequest)
                        .exchange()
                        .expectStatus().isNotFound()
                        .expectBody(String.class)
                        .returnResult().getResponseBody()
        ).contains("User not found by id: " + badId);

        assertThat(
                restTestClient.post()
                        .uri(API_PREFIX + "/oauth2-user/" + VALID_OAUTH_USER_ID + "/password-reset")
                        .header("cookie", jSessionId)
                        .body(passwordChangeRequest)
                        .exchange()
                        .expectStatus().isBadRequest()
                        .expectBody(String.class)
                        .returnResult().getResponseBody()
        ).contains("New password and new password 2 do not match");

        passwordChangeRequest = new PasswordChangeRequest(
                "password",
                "password",
                "password"
        );
        var user = restTestClient.post()
                .uri(API_PREFIX + "/oauth2-user/" + VALID_OAUTH_USER_ID + "/password-reset")
                .header("cookie", jSessionId)
                .body(passwordChangeRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Oauth2UsersService.OauthUserDto.class)
                .returnResult()
                .getResponseBody();
        assertThat(user).isNotNull();

        passwordChangeRequest = new PasswordChangeRequest(
                "password",
                "", // set to empty to make random
                ""
        );
        user = restTestClient.post()
                .uri(API_PREFIX + "/oauth2-user/" + VALID_OAUTH_USER_ID + "/password-reset")
                .header("cookie", jSessionId)
                .body(passwordChangeRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Oauth2UsersService.OauthUserDto.class)
                .returnResult()
                .getResponseBody();
        assertThat(user).isNotNull();
    }

    @Test
    void testGenerate2FaQrCodeImage() {
        val pngSignature = new byte[]{
                (byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47,
                (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A
        };
        val qrCodePng = restTestClient.get()
                .uri(API_PREFIX + "/oauth2-user/" + VALID_OAUTH_USER_ID + "/2fa-qr-code")
                .header("cookie", jSessionId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(byte[].class)
                .returnResult()
                .getResponseBody();
        assertThat(qrCodePng).isNotNull().startsWith(pngSignature);

        val badUserId = createId();
        assertThat(
                restTestClient.get()
                        .uri(API_PREFIX + "/oauth2-user/" + badUserId + "/2fa-qr-code")
                        .header("cookie", jSessionId)
                        .exchange()
                        .expectStatus().isNotFound()
                        .expectBody(String.class)
                        .returnResult()
                        .getResponseBody()
        ).contains("User not found by id: " + badUserId);
    }

    @Test
    void testUpdateOauth2UserById() {
        val user = createOauth2User();
        var updateOauthUserRequest = new OauthUserRequest(
                user.getId(),
                user.getUsername(),
                "password",
                true,
                "{\"test\":123}",
                false
        );
        restTestClient.post()
                .uri(API_PREFIX + "/oauth2-user/" + user.getId())
                .header("cookie", jSessionId)
                .body(updateOauthUserRequest)
                .exchange()
                .expectStatus().isOk();

        val updatedUser = restTestClient.get()
                .uri(API_PREFIX + "/oauth2-user/" + user.getId())
                .header("cookie", jSessionId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Oauth2UsersService.OauthUserDto.class)
                .returnResult()
                .getResponseBody();
        assertThat(updatedUser).isNotNull();
        assertThat(updatedUser.getMetadata()).isEqualTo("{\"test\":123}");
        assertThat(updatedUser.isEnabled()).isTrue();

        val badUserId = createId();
        assertThat(
                restTestClient.post()
                        .uri(API_PREFIX + "/oauth2-user/" + badUserId)
                        .header("cookie", jSessionId)
                        .body(updateOauthUserRequest)
                        .exchange()
                        .expectStatus().isNotFound()
                        .expectBody(String.class)
                        .returnResult()
                        .getResponseBody()
        ).contains("User not found by id: " + badUserId);

        updateOauthUserRequest = new OauthUserRequest(
                user.getId(),
                user.getUsername(),
                "password",
                false,
                "{\"test\":123}",
                false
        );
        restTestClient.post()
                .uri(API_PREFIX + "/oauth2-user/" + user.getId())
                .header("cookie", jSessionId)
                .body(updateOauthUserRequest)
                .exchange()
                .expectStatus().isOk();

        val updatedUser2 = restTestClient.get()
                .uri(API_PREFIX + "/oauth2-user/" + user.getId())
                .header("cookie", jSessionId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Oauth2UsersService.OauthUserDto.class)
                .returnResult()
                .getResponseBody();
        assertThat(updatedUser2).isNotNull();
        assertThat(updatedUser2.getMetadata()).isEqualTo("{\"test\":123}");
        assertThat(updatedUser2.isEnabled()).isFalse();

        updateOauthUserRequest = new OauthUserRequest(
                user.getId(),
                "user1", // username is taken already
                "password",
                false,
                "{\"test\":123}",
                false
        );
        assertThat(
                restTestClient.post()
                        .uri(API_PREFIX + "/oauth2-user/" + user.getId())
                        .header("cookie", jSessionId)
                        .body(updateOauthUserRequest)
                        .exchange()
                        .expectStatus().isBadRequest()
                        .expectBody(String.class)
                        .returnResult().getResponseBody()
        ).contains("Username already exists: user1");
    }

    @Test
    void testCreateOauth2User() {
        val user = createOauth2User();
        assertThat(user.getOrganizationId()).isEqualTo(VALID_ORGANIZATION_ID);
        assertThat(user.getMetadata()).isEqualTo("{}");
        assertThat(user.getCreateTime()).isAfter(Instant.now().minusSeconds(10));
        oauthUserRepository.deleteById(user.getId());

        var oauthUserRequest = new OauthUserRequest(
                createId(),
                "", // empty username will fail
                "password",
                true,
                "{}",
                false
        );
        assertThat(
                restTestClient.post()
                        .uri(API_PREFIX + "/oauth2-user")
                        .header("cookie", jSessionId)
                        .body(oauthUserRequest)
                        .exchange()
                        .expectStatus().isBadRequest()
                        .expectBody(String.class)
                        .returnResult()
                        .getResponseBody()
        ).contains("Username can not be empty");

        oauthUserRequest = new OauthUserRequest(
                createId(),
                "user1", // username is already taken within that organization
                "password",
                true,
                "{}",
                false
        );
        assertThat(
                restTestClient.post()
                        .uri(API_PREFIX + "/oauth2-user")
                        .header("cookie", jSessionId)
                        .body(oauthUserRequest)
                        .exchange()
                        .expectStatus().isBadRequest()
                        .expectBody(String.class)
                        .returnResult()
                        .getResponseBody()
        ).contains("Username already exists: user1");
    }

    @Test
    void testDeleteUsers() {
        val user = createOauth2User();
        val deleteUsersRequest = new DeleteUsersRequest(List.of(user.getId()));
        restTestClient.method(HttpMethod.DELETE)
                .uri(API_PREFIX + "/oauth2-user")
                .header("cookie", jSessionId)
                .body(deleteUsersRequest)
                .exchange()
                .expectStatus().isOk();

        assertThat(
                restTestClient.get()
                        .uri(API_PREFIX + "/oauth2-user/" + user.getId())
                        .header("cookie", jSessionId)
                        .exchange()
                        .expectStatus().isNotFound()
                        .expectBody(String.class)
                        .returnResult().getResponseBody()
        ).contains("ser not found by id: " + user.getId());

        assertThat(
                restTestClient.method(HttpMethod.DELETE)
                        .uri(API_PREFIX + "/oauth2-user")
                        .header("cookie", jSessionId)
                        .body(new DeleteUsersRequest(List.of())) // empty list will fail
                        .exchange()
                        .expectStatus().isBadRequest()
                        .expectBody(String.class)
                        .returnResult().getResponseBody()
        ).contains("User IDs can not be empty");

        val badUserId = createId();
        assertThat(
                restTestClient.method(HttpMethod.DELETE)
                        .uri(API_PREFIX + "/oauth2-user")
                        .header("cookie", jSessionId)
                        .body(new DeleteUsersRequest(List.of(badUserId))) // bad user id in list will fail
                        .exchange()
                        .expectStatus().isNotFound()
                        .expectBody(String.class)
                        .returnResult().getResponseBody()
        ).contains("User not found by id: " + badUserId);
    }

    private Oauth2UsersService.OauthUserDto createOauth2User() {
        val oauthUserRequest = new OauthUserRequest(
                createId(),
                "user-" + RandomStringUtils.secure().nextAlphabetic(10),
                "password",
                true,
                "{}",
                false
        );
        val user = restTestClient.post()
                .uri(API_PREFIX + "/oauth2-user")
                .header("cookie", jSessionId)
                .body(oauthUserRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Oauth2UsersService.OauthUserDto.class)
                .returnResult()
                .getResponseBody();
        assertThat(user).isNotNull();
        return user;
    }
}