package com.authbox.web.controller;

import com.authbox.base.dao.AccessLogRepository;
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
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

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
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AccessLogController accessLogController;
    @Autowired
    private AccessLogRepository accessLogRepository;

    private String jSessionId;

    @BeforeEach
    public void setup() {
        // setup http client with no redirect
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
        val updateUserRequest = new UpdateUserRequest(
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
        val passwordChangeRequest = new PasswordChangeRequest(
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
                .containsKey("page");
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
    }

    @Test
    public void testUpdateAccount() {
        val updateUserRequest = new UpdateUserRequest(
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