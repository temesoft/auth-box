package com.authbox.web.controller;

import com.authbox.base.model.ErrorResponse;
import com.authbox.web.Application;
import com.authbox.web.TestUtils;
import com.authbox.web.model.CreateAccountRequest;
import com.authbox.web.model.DeleteAccountsRequest;
import com.authbox.web.model.PasswordChangeRequest;
import com.authbox.web.model.UpdateUserRequest;
import com.authbox.web.model.UserDto;
import com.authbox.web.model.UserRole;
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

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.authbox.base.util.IdUtils.createId;
import static com.authbox.web.TestConstants.VALID_ORGANIZATION_ID;
import static com.authbox.web.TestConstants.VALID_PASSWORD;
import static com.authbox.web.TestConstants.VALID_USERNAME;
import static com.authbox.web.TestConstants.VALID_USER_ID;
import static com.authbox.web.config.Constants.API_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = {Application.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureRestTestClient
@TestPropertySource(locations = "classpath:application-test.properties")
class AccountControllerTest {

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
    public void testGetCurrentAccount() {
        val user = restTestClient.get()
                .uri(API_PREFIX + "/account")
                .header("cookie", jSessionId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(UserDto.class)
                .returnResult()
                .getResponseBody();
        assertThat(user).isNotNull();
        assertThat(user.getId()).isEqualTo(VALID_USER_ID);
        assertThat(user.getCreateTime()).isNotNull();
        assertThat(user.getLastUpdated()).isNotNull();
        assertThat(user.getUsername()).isEqualTo(VALID_USERNAME);
        assertThat(user.getName()).isEqualTo("Mr. Admin");
        assertThat(user.getRoles()).contains("ROLE_ADMIN", "ROLE_USER");
        assertThat(user.isEnabled()).isEqualTo(true);
        assertThat(user.isAdmin()).isEqualTo(true);
        assertThat(user.getOrganizationId()).isEqualTo(VALID_ORGANIZATION_ID);
    }

    @Test
    public void testUpdateCurrentAccount() {
        var updateUserRequest = new UpdateUserRequest(
                VALID_USER_ID,
                VALID_USERNAME,
                VALID_PASSWORD,
                "Mr. Admin",
                true,
                List.of(
                        UserRole.ROLE_USER.name(),
                        UserRole.ROLE_ADMIN.name()
                )
        );
        val currentUser = restTestClient.post()
                .uri(API_PREFIX + "/account")
                .header("cookie", jSessionId)
                .body(updateUserRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(UserDto.class)
                .returnResult()
                .getResponseBody();
        assertThat(currentUser).isNotNull();
        assertThat(currentUser.getId()).isEqualTo(VALID_USER_ID);
        assertThat(currentUser.getCreateTime()).isNotNull();
        assertThat(currentUser.getLastUpdated()).isNotNull();
        assertThat(currentUser.getUsername()).isEqualTo(VALID_USERNAME);
        assertThat(currentUser.getName()).isEqualTo("Mr. Admin");
        assertThat(currentUser.getRoles()).contains("ROLE_ADMIN", "ROLE_USER");
        assertThat(currentUser.isEnabled()).isEqualTo(true);
        assertThat(currentUser.isAdmin()).isEqualTo(true);
        assertThat(currentUser.getOrganizationId()).isEqualTo(VALID_ORGANIZATION_ID);
    }

    @Test
    public void testUpdateCurrentAccountPassword() {
        var passwordChangeRequest = new PasswordChangeRequest(
                VALID_PASSWORD,
                VALID_PASSWORD,
                VALID_PASSWORD
        );
        val currentUser = restTestClient.post()
                .uri(API_PREFIX + "/account/password")
                .header("cookie", jSessionId)
                .body(passwordChangeRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(UserDto.class)
                .returnResult()
                .getResponseBody();
        assertThat(currentUser).isNotNull();
        assertThat(currentUser.getId()).isEqualTo(VALID_USER_ID);
        assertThat(currentUser.getCreateTime()).isNotNull();
        assertThat(currentUser.getLastUpdated()).isNotNull();
        assertThat(currentUser.getUsername()).isEqualTo(VALID_USERNAME);
        assertThat(currentUser.getName()).isEqualTo("Mr. Admin");
        assertThat(currentUser.getRoles()).contains("ROLE_ADMIN", "ROLE_USER");
        assertThat(currentUser.isEnabled()).isEqualTo(true);
        assertThat(currentUser.isAdmin()).isEqualTo(true);
        assertThat(currentUser.getOrganizationId()).isEqualTo(VALID_ORGANIZATION_ID);

        passwordChangeRequest = new PasswordChangeRequest(
                "", // empty original password will trigger failure
                VALID_PASSWORD,
                VALID_PASSWORD
        );

        assertThat(
                Objects.requireNonNull(restTestClient.post()
                        .uri(API_PREFIX + "/account/password")
                        .header("cookie", jSessionId)
                        .body(passwordChangeRequest)
                        .exchange()
                        .expectStatus().isBadRequest()
                        .expectBody(ErrorResponse.class)
                        .returnResult()
                        .getResponseBody())
                        .message
        ).contains("Old password can not be empty");

        passwordChangeRequest = new PasswordChangeRequest(
                "incorrect-original-password", // incorrect original password will trigger failure
                VALID_PASSWORD,
                VALID_PASSWORD
        );
        assertThat(
                Objects.requireNonNull(restTestClient.post()
                        .uri(API_PREFIX + "/account/password")
                        .header("cookie", jSessionId)
                        .body(passwordChangeRequest)
                        .exchange()
                        .expectStatus().isBadRequest()
                        .expectBody(ErrorResponse.class)
                        .returnResult()
                        .getResponseBody())
                        .message
        ).contains("Old password does not match");

        passwordChangeRequest = new PasswordChangeRequest(
                VALID_PASSWORD,
                "",
                "" // empty password will trigger failure
        );
        assertThat(
                Objects.requireNonNull(restTestClient.post()
                        .uri(API_PREFIX + "/account/password")
                        .header("cookie", jSessionId)
                        .body(passwordChangeRequest)
                        .exchange()
                        .expectStatus().isBadRequest()
                        .expectBody(ErrorResponse.class)
                        .returnResult()
                        .getResponseBody())
                        .message
        ).contains("New password is empty or shorter than 6 characters");

        passwordChangeRequest = new PasswordChangeRequest(
                VALID_PASSWORD,
                "password1",
                "password2" // second password does not match first will trigger failure
        );
        assertThat(
                Objects.requireNonNull(restTestClient.post()
                        .uri(API_PREFIX + "/account/password")
                        .header("cookie", jSessionId)
                        .body(passwordChangeRequest)
                        .exchange()
                        .expectStatus().isBadRequest()
                        .expectBody(ErrorResponse.class)
                        .returnResult()
                        .getResponseBody())
                        .message
        ).contains("New password and new password 2 do not match");
    }

    @Test
    public void testListAccounts() {
        val pageOfUsers = restTestClient.get()
                .uri(API_PREFIX + "/account/list")
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
                .containsKey("pageable");
    }

    @Test
    public void testGetAccount() {
        val user = createAccountForOrganization();
        val userFound = restTestClient.get()
                .uri(API_PREFIX + "/account/" + user.getId())
                .header("cookie", jSessionId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(UserDto.class)
                .returnResult()
                .getResponseBody();
        assertThat(userFound).isNotNull();
        assertThat(userFound.getId()).isEqualTo(user.getId());
        assertThat(userFound.getUsername()).isEqualTo(user.getUsername());
        assertThat(userFound.getPassword()).isEqualTo(user.getPassword());

        val badUserId = createId();
        assertThat(
                Objects.requireNonNull(restTestClient.get()
                        .uri(API_PREFIX + "/account/" + badUserId)
                        .header("cookie", jSessionId)
                        .exchange()
                        .expectStatus().isNotFound()
                        .expectBody(ErrorResponse.class)
                        .returnResult()
                        .getResponseBody())
                        .message
        ).contains("User not found by id: " + badUserId);
    }

    @Test
    public void testUpdateAccount() {
        var updateUserRequest = new UpdateUserRequest(
                VALID_USER_ID,
                VALID_USERNAME,
                VALID_PASSWORD,
                "Mr. Admin",
                true,
                List.of(
                        UserRole.ROLE_USER.name(),
                        UserRole.ROLE_ADMIN.name()
                )
        );
        restTestClient.post()
                .uri(API_PREFIX + "/account/" + VALID_USER_ID)
                .header("cookie", jSessionId)
                .body(updateUserRequest)
                .exchange()
                .expectStatus().isOk();

        val badUserId = createId();
        assertThat(
                Objects.requireNonNull(restTestClient.post()
                        .uri(API_PREFIX + "/account/" + badUserId)
                        .header("cookie", jSessionId)
                        .body(updateUserRequest)
                        .exchange()
                        .expectStatus().isNotFound()
                        .expectBody(ErrorResponse.class)
                        .returnResult().getResponseBody())
                        .message
        ).contains("User not found by id: " + badUserId);


        updateUserRequest = new UpdateUserRequest(
                VALID_USER_ID,
                "", // empty username will trigger failure
                VALID_PASSWORD,
                "Mr. Admin",
                true,
                List.of(
                        UserRole.ROLE_USER.name(),
                        UserRole.ROLE_ADMIN.name()
                )
        );
        assertThat(
                Objects.requireNonNull(restTestClient.post()
                        .uri(API_PREFIX + "/account/" + VALID_USER_ID)
                        .header("cookie", jSessionId)
                        .body(updateUserRequest)
                        .exchange()
                        .expectStatus().isBadRequest()
                        .expectBody(ErrorResponse.class)
                        .returnResult().getResponseBody())
                        .message
        ).contains("Account username can not be empty");

        updateUserRequest = new UpdateUserRequest(
                VALID_USER_ID,
                VALID_USERNAME,
                VALID_PASSWORD,
                "", // empty name will trigger failure
                true,
                List.of(
                        UserRole.ROLE_USER.name(),
                        UserRole.ROLE_ADMIN.name()
                )
        );
        assertThat(
                Objects.requireNonNull(restTestClient.post()
                        .uri(API_PREFIX + "/account/" + VALID_USER_ID)
                        .header("cookie", jSessionId)
                        .body(updateUserRequest)
                        .exchange()
                        .expectStatus().isBadRequest()
                        .expectBody(ErrorResponse.class)
                        .returnResult().getResponseBody())
                        .message
        ).contains("Account name can not be empty");

        updateUserRequest = new UpdateUserRequest(
                VALID_USER_ID,
                VALID_USERNAME,
                VALID_PASSWORD,
                "Mr. Admin",
                true,
                List.of() // empty roles will trigger failure
        );
        assertThat(
                Objects.requireNonNull(restTestClient.post()
                        .uri(API_PREFIX + "/account/" + VALID_USER_ID)
                        .header("cookie", jSessionId)
                        .body(updateUserRequest)
                        .exchange()
                        .expectStatus().isBadRequest()
                        .expectBody(ErrorResponse.class)
                        .returnResult().getResponseBody())
                        .message
        ).contains("Account access roles can not be empty");
    }

    @Test
    public void testDeleteAccounts() {
        val user = createAccountForOrganization();
        val deleteAccountsRequest = new DeleteAccountsRequest(List.of(user.getId()));
        restTestClient.method(HttpMethod.DELETE)
                .uri(API_PREFIX + "/account")
                .header("cookie", jSessionId)
                .body(deleteAccountsRequest)
                .exchange()
                .expectStatus().isOk();

        restTestClient.get()
                .uri(API_PREFIX + "/account/" + user.getId())
                .header("cookie", jSessionId)
                .exchange()
                .expectStatus().isNotFound();

        assertThat(
                Objects.requireNonNull(restTestClient.method(HttpMethod.DELETE)
                        .uri(API_PREFIX + "/account")
                        .header("cookie", jSessionId)
                        .body(new DeleteAccountsRequest(List.of(VALID_USER_ID)))
                        .exchange()
                        .expectStatus().isBadRequest()
                        .expectBody(ErrorResponse.class)
                        .returnResult().getResponseBody())
                        .message
        ).contains("User is unable to remove self");

        val badUserId = createId();
        assertThat(
                Objects.requireNonNull(restTestClient.method(HttpMethod.DELETE)
                        .uri(API_PREFIX + "/account")
                        .header("cookie", jSessionId)
                        .body(new DeleteAccountsRequest(List.of(badUserId)))
                        .exchange()
                        .expectStatus().isNotFound()
                        .expectBody(ErrorResponse.class)
                        .returnResult().getResponseBody())
                        .message
        ).contains("User not found by id: " + badUserId);
    }

    @Test
    public void testCreateAccountForOrganization() {
        val user = createAccountForOrganization();
        assertThat(user).isNotNull();
        assertThat(user.getId()).isNotBlank().isNotEqualTo(VALID_USER_ID);
        assertThat(user.getCreateTime()).isNotNull();
        assertThat(user.getLastUpdated()).isNotNull();
        assertThat(user.getUsername()).isNotBlank();
        assertThat(user.getName()).isNotBlank();
        assertThat(user.getRoles()).contains(UserRole.ROLE_USER.name());
        assertThat(user.isEnabled()).isEqualTo(true);
        assertThat(user.isAdmin()).isEqualTo(false);
        assertThat(user.getOrganizationId()).isEqualTo(VALID_ORGANIZATION_ID);

        var createAccountRequest = new CreateAccountRequest(
                createId(),
                "", // empty username will trigger failure
                "password",
                "Tester",
                UserRole.ROLE_USER
        );
        assertThat(
                Objects.requireNonNull(restTestClient.post()
                        .uri(API_PREFIX + "/account/create")
                        .header("cookie", jSessionId)
                        .body(createAccountRequest)
                        .exchange()
                        .expectStatus().isBadRequest()
                        .expectBody(ErrorResponse.class)
                        .returnResult()
                        .getResponseBody())
                        .message
        ).contains("Account username can not be empty");

        createAccountRequest = new CreateAccountRequest(
                createId(),
                "test-" + RandomStringUtils.secure().nextAlphabetic(10),
                "password",
                "", // empty name will trigger failure
                UserRole.ROLE_USER
        );
        assertThat(
                Objects.requireNonNull(restTestClient.post()
                        .uri(API_PREFIX + "/account/create")
                        .header("cookie", jSessionId)
                        .body(createAccountRequest)
                        .exchange()
                        .expectStatus().isBadRequest()
                        .expectBody(ErrorResponse.class)
                        .returnResult()
                        .getResponseBody())
                        .message
        ).contains("Account name can not be empty");

        createAccountRequest = new CreateAccountRequest(
                createId(),
                "admin", // username taken
                "password",
                "Mr. Admin",
                UserRole.ROLE_USER
        );
        assertThat(
                Objects.requireNonNull(restTestClient.post()
                        .uri(API_PREFIX + "/account/create")
                        .header("cookie", jSessionId)
                        .body(createAccountRequest)
                        .exchange()
                        .expectStatus().isBadRequest()
                        .expectBody(ErrorResponse.class)
                        .returnResult()
                        .getResponseBody())
                        .message
        ).contains("This username is taken, please select another user");

        createAccountRequest = new CreateAccountRequest(
                createId(),
                "test-" + RandomStringUtils.secure().nextAlphabetic(10),
                "password",
                "Mr. Admin",
                null // role can not be null - will trigger failure
        );
        assertThat(
                Objects.requireNonNull(restTestClient.post()
                        .uri(API_PREFIX + "/account/create")
                        .header("cookie", jSessionId)
                        .body(createAccountRequest)
                        .exchange()
                        .expectStatus().isBadRequest()
                        .expectBody(ErrorResponse.class)
                        .returnResult()
                        .getResponseBody())
                        .message
        ).contains("Account access roles can not be empty");
    }

    private UserDto createAccountForOrganization() {
        val createAccountRequest = new CreateAccountRequest(
                createId(),
                "test-" + RandomStringUtils.secure().nextAlphabetic(10),
                "password",
                "Tester",
                UserRole.ROLE_USER
        );
        return restTestClient.post()
                .uri(API_PREFIX + "/account/create")
                .header("cookie", jSessionId)
                .body(createAccountRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(UserDto.class)
                .returnResult()
                .getResponseBody();
    }
}