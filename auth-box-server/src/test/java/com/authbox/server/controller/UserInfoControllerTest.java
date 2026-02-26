package com.authbox.server.controller;

import com.authbox.base.dao.OauthClientDao;
import com.authbox.base.dao.OauthClientRepository;
import com.authbox.base.dao.OauthTokenDao;
import com.authbox.base.dao.OauthTokenRepository;
import com.authbox.base.dao.OauthUserDao;
import com.authbox.base.dao.OauthUserRepository;
import com.authbox.base.dao.OrganizationDao;
import com.authbox.base.dao.OrganizationRepository;
import com.authbox.base.model.GrantType;
import com.authbox.base.model.OauthClient;
import com.authbox.base.model.OauthToken;
import com.authbox.base.model.OauthUser;
import com.authbox.base.model.Organization;
import com.authbox.base.model.TokenFormat;
import com.authbox.base.model.TokenType;
import com.authbox.base.service.AccessLogService;
import com.authbox.server.Application;
import lombok.val;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static com.authbox.base.config.Constants.HEADER_AUTHORIZATION;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_ACCESS_TOKEN;
import static com.authbox.base.config.Constants.OAUTH_PREFIX;
import static com.authbox.base.util.HashUtils.sha256;
import static com.authbox.base.util.IdUtils.createId;
import static com.authbox.server.TestUtils.assertLogEntryContainsAndReset;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SpringBootTest(
        classes = {Application.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureRestTestClient
@TestPropertySource(locations = "classpath:application-test.properties")
class UserInfoControllerTest {

    private static final String SCOPE = "some/scope";
    private static final String OAUTH_CLIENT_ID = createId();
    private static final String OAUTH_USER_ID = createId();
    private static final String OAUTH_TOKEN_ID = createId();

    @Autowired
    private OauthTokenDao oauthTokenDao;
    @Autowired
    private OauthTokenRepository oauthTokenRepository;
    @Autowired
    private OauthClientDao oauthClientDao;
    @Autowired
    private OauthClientRepository oauthClientRepository;
    @Autowired
    private OauthUserDao oauthUserDao;
    @Autowired
    private OauthUserRepository oauthUserRepository;
    @Autowired
    private OrganizationDao organizationDao;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private RestTestClient restClient;
    @MockitoSpyBean
    private AccessLogService accessLogService;

    private Organization presetOrg;

    @BeforeEach
    public void setup() {
        presetOrg = organizationRepository.findById("c1ade6b3-c023-44f4-b3ef-b0f27ba5e6e8").orElseThrow();
    }

    @AfterEach
    public void teardown() {
        oauthClientRepository.deleteById(OAUTH_CLIENT_ID);
        oauthUserRepository.deleteById(OAUTH_USER_ID);
        organizationRepository.deleteAll();
        organizationRepository.save(presetOrg);
    }

    @Test
    public void testGetUserInfo() {
        assertThat(
                restClient.get()
                        .uri(OAUTH_PREFIX + "/user")
                        .header("content-type", APPLICATION_JSON_VALUE)
                        .exchange()
                        .expectStatus().is4xxClientError()
                        .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {
                        })
                        .returnResult().getResponseBody())
                .containsEntry("error", "Unauthorized")
                .containsEntry("message", "unauthorized request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Processing user info request",
                "Access token is not provided"
        );

        val badToken = "bad-token";
        val badTokenHash = sha256(badToken);
        assertThat(
                restClient.get()
                        .uri(OAUTH_PREFIX + "/user")
                        .header("content-type", APPLICATION_JSON_VALUE)
                        .header(HEADER_AUTHORIZATION, badToken)
                        .exchange()
                        .expectStatus().is4xxClientError()
                        .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {
                        })
                        .returnResult().getResponseBody())
                .containsEntry("error", "Unauthorized")
                .containsEntry("message", "invalid token");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Processing user info request",
                "Access token='%s' / hash='%s' not found".formatted(badToken, badTokenHash)
        );

        assertThat(
                restClient.get()
                        .uri(builder ->
                                builder.path(OAUTH_PREFIX + "/user")
                                        .queryParam(OAUTH2_ATTR_ACCESS_TOKEN, "bad-token")
                                        .build()
                        )
                        .header("content-type", APPLICATION_JSON_VALUE)
                        .exchange()
                        .expectStatus().is4xxClientError()
                        .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {
                        })
                        .returnResult().getResponseBody())
                .containsEntry("error", "Unauthorized")
                .containsEntry("message", "invalid token");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Processing user info request",
                "Access token='%s' / hash='%s' not found".formatted(badToken, badTokenHash)
        );

        organizationRepository.deleteAll(); // delete localhost based org
        val organization = Organization.builder()
                .withId(createId())
                .withCreateTime(Instant.now())
                .withName("localhost")
                .withDomainPrefix("localhost")
                .withAddress("101 Main")
                .withEnabled(true)
                .withLastUpdated(Instant.now())
                .build();
        organizationDao.insert(organization);

        val client = new OauthClient(
                OAUTH_CLIENT_ID,
                Instant.now(),
                "description",
                "secret",
                List.of(GrantType.client_credentials, GrantType.refresh_token),
                organization.getId(),
                false, // disabled initially
                List.of("https://some.domain/redirect1", "https://other.domain/redirect2"),
                Duration.ofHours(1),
                Duration.ofHours(2),
                TokenFormat.STANDARD,
                "priv-key",
                "pub-key",
                Instant.now(),
                List.of(),
                List.of()
        );
        oauthClientDao.insert(client);

        val username = RandomStringUtils.secure().nextAlphabetic(20);
        val oauthUser = OauthUser.builder()
                .withId(OAUTH_USER_ID)
                .withCreateTime(Instant.now())
                .withUsername(username)
                .withOrganizationId(organization.getId())
                .withPassword("password")
                .withMetadata("{\"test\":123}")
                .withSecret(createId())
                .withLastUpdated(Instant.now())
                .build();
        oauthUserDao.insert(oauthUser);

        val goodToken = "good-token";
        val goodTokenHash = sha256(goodToken);
        val oauthToken = OauthToken.builder()
                .withId(OAUTH_TOKEN_ID)
                .withCreateTime(Instant.now())
                .withExpiration(Instant.now().plusSeconds(10))
                .withOrganizationId("invalid-org-id")
                .withHash(goodTokenHash)
                .withClientId("bad-client-id") // bad client id initially
                .withOauthUserId(null) // no user initially
                .withScopes(List.of(SCOPE))
                .withTokenType(TokenType.AUTHORIZATION_CODE) // not access_token initially
                .withIp("1.2.3.4")
                .withUserAgent("Some-User-Agent")
                .withRequestId(createId())
                .build();
        oauthTokenDao.insert(oauthToken);

        assertThat(
                restClient.get()
                        .uri(builder ->
                                builder.path(OAUTH_PREFIX + "/user")
                                        .queryParam(OAUTH2_ATTR_ACCESS_TOKEN, goodToken)
                                        .build()
                        )
                        .header("content-type", APPLICATION_JSON_VALUE)
                        .exchange()
                        .expectStatus().is4xxClientError()
                        .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {
                        })
                        .returnResult().getResponseBody())
                .containsEntry("error", "Unauthorized")
                .containsEntry("message", "invalid token");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Processing user info request",
                "Provided token is not ACCESS_TOKEN. type='%s' token='%s' / hash='%s'"
                        .formatted(TokenType.AUTHORIZATION_CODE, goodToken, goodTokenHash)
        );

        oauthToken.setTokenType(TokenType.ACCESS_TOKEN);
        oauthTokenRepository.save(oauthToken);
        assertThat(
                restClient.get()
                        .uri(builder ->
                                builder.path(OAUTH_PREFIX + "/user")
                                        .queryParam(OAUTH2_ATTR_ACCESS_TOKEN, goodToken)
                                        .build()
                        )
                        .header("content-type", APPLICATION_JSON_VALUE)
                        .exchange()
                        .expectStatus().is4xxClientError()
                        .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {
                        })
                        .returnResult().getResponseBody())
                .containsEntry("error", "Unauthorized")
                .containsEntry("message", "invalid token");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Processing user info request",
                "Token does not belong to organization id='%s'".formatted(organization.getId())
        );

        oauthToken.setOrganizationId(organization.getId());
        oauthTokenRepository.save(oauthToken);
        assertThat(
                restClient.get()
                        .uri(builder ->
                                builder.path(OAUTH_PREFIX + "/user")
                                        .queryParam(OAUTH2_ATTR_ACCESS_TOKEN, goodToken)
                                        .build()
                        )
                        .header("content-type", APPLICATION_JSON_VALUE)
                        .exchange()
                        .expectStatus().is4xxClientError()
                        .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {
                        })
                        .returnResult().getResponseBody())
                .containsEntry("error", "Unauthorized")
                .containsEntry("message", "invalid token");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Processing user info request",
                "Access token with hash='%s' is not linked to a oauth user".formatted(goodTokenHash)
        );

        oauthToken.setOauthUserId("bad-user-id");
        oauthTokenRepository.save(oauthToken);
        assertThat(
                restClient.get()
                        .uri(builder ->
                                builder.path(OAUTH_PREFIX + "/user")
                                        .queryParam(OAUTH2_ATTR_ACCESS_TOKEN, goodToken)
                                        .build()
                        )
                        .header("content-type", APPLICATION_JSON_VALUE)
                        .exchange()
                        .expectStatus().is4xxClientError()
                        .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {
                        })
                        .returnResult().getResponseBody())
                .containsEntry("error", "Unauthorized")
                .containsEntry("message", "unauthorized request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Processing user info request",
                "Oauth2 user not found by id='bad-user-id'"
        );

        oauthToken.setOauthUserId(oauthUser.getId());
        oauthTokenRepository.save(oauthToken);
        assertThat(
                restClient.get()
                        .uri(builder ->
                                builder.path(OAUTH_PREFIX + "/user")
                                        .queryParam(OAUTH2_ATTR_ACCESS_TOKEN, goodToken)
                                        .build()
                        )
                        .header("content-type", APPLICATION_JSON_VALUE)
                        .exchange()
                        .expectStatus().is4xxClientError()
                        .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {
                        })
                        .returnResult().getResponseBody())
                .containsEntry("error", "Unauthorized")
                .containsEntry("message", "unauthorized request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Processing user info request",
                "Oauth2 user is disabled"
        );

        oauthUser.setEnabled(true);
        oauthUserRepository.save(oauthUser);
        assertThat(
                restClient.get()
                        .uri(builder ->
                                builder.path(OAUTH_PREFIX + "/user")
                                        .queryParam(OAUTH2_ATTR_ACCESS_TOKEN, goodToken)
                                        .build()
                        )
                        .header("content-type", APPLICATION_JSON_VALUE)
                        .exchange()
                        .expectStatus().is4xxClientError()
                        .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {
                        })
                        .returnResult().getResponseBody())
                .containsEntry("error", "Unauthorized")
                .containsEntry("message", "unauthorized request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Processing user info request",
                "Oauth2 client not found by id='bad-client-id'"
        );

        oauthToken.setClientId(client.getId());
        oauthTokenRepository.save(oauthToken);
        assertThat(
                restClient.get()
                        .uri(builder ->
                                builder.path(OAUTH_PREFIX + "/user")
                                        .queryParam(OAUTH2_ATTR_ACCESS_TOKEN, goodToken)
                                        .build()
                        )
                        .header("content-type", APPLICATION_JSON_VALUE)
                        .exchange()
                        .expectStatus().is4xxClientError()
                        .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {
                        })
                        .returnResult().getResponseBody())
                .containsEntry("error", "Unauthorized")
                .containsEntry("message", "unauthorized request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Processing user info request",
                "OauthClient is disabled. id='%s'".formatted(client.getId())
        );

        client.setEnabled(true);
        oauthClientRepository.save(client);
        assertThat(
                restClient.get()
                        .uri(builder ->
                                builder.path(OAUTH_PREFIX + "/user")
                                        .queryParam(OAUTH2_ATTR_ACCESS_TOKEN, goodToken)
                                        .build()
                        )
                        .header("content-type", APPLICATION_JSON_VALUE)
                        .exchange()
                        .expectStatus().is2xxSuccessful()
                        .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {
                        })
                        .returnResult().getResponseBody())
                .containsKey("id")
                .containsKey("metadata")
                .containsEntry("username", username)
                .containsEntry("organization_id", organization.getId());
        assertLogEntryContainsAndReset(
                accessLogService,
                "Processing user info request",
                "User info request finished"
        );
    }
}