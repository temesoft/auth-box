package com.authbox.web.controller;

import com.authbox.base.dao.OauthClientRepository;
import com.authbox.base.model.GrantType;
import com.authbox.base.model.TokenFormat;
import com.authbox.base.model.UpdateOauthClientRequest;
import com.authbox.base.util.CertificateKeysUtils;
import com.authbox.web.Application;
import com.authbox.web.TestUtils;
import com.authbox.web.model.DeleteClientsRequest;
import com.authbox.web.service.Oauth2ClientsService;
import lombok.val;
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
import org.springframework.util.LinkedMultiValueMap;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static com.authbox.base.util.IdUtils.createId;
import static com.authbox.web.TestConstants.VALID_CLIENT_ID;
import static com.authbox.web.TestConstants.VALID_ORGANIZATION_ID;
import static com.authbox.web.config.Constants.API_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = {Application.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureRestTestClient
@TestPropertySource(locations = "classpath:application-test.properties")
class Oauth2ClientsControllerTest {

    @LocalServerPort
    private int port;
    @Autowired
    private RestTestClient restTestClient;
    @Autowired
    private OauthClientRepository oauthClientRepository;

    private String jSessionId;

    @BeforeEach
    public void setup() {
        jSessionId = TestUtils.authenticateAccountGetCookie(port);
    }

    @Test
    public void testGetOauth2Clients() {
        val pageOfClients = restTestClient.get()
                .uri(API_PREFIX + "/oauth2-client")
                .header("cookie", jSessionId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .returnResult()
                .getResponseBody();
        assertThat(pageOfClients)
                .isNotNull()
                .containsKey("content")
                .containsKey("page");
    }

    @Test
    public void testGetOauth2ClientById() {
        val client = restTestClient.get()
                .uri(API_PREFIX + "/oauth2-client/" + VALID_CLIENT_ID)
                .header("cookie", jSessionId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Oauth2ClientsService.OauthClientDto.class)
                .returnResult()
                .getResponseBody();
        assertThat(client).isNotNull();
        assertThat(client.getId()).isEqualTo(VALID_CLIENT_ID);
        assertThat(client.getGrantTypes()).contains(
                GrantType.client_credentials,
                GrantType.password,
                GrantType.authorization_code,
                GrantType.refresh_token
        );
    }

    @Test
    public void testCreateOauth2Client() {
        val client = createOauth2Client();
        assertThat(client).isNotNull();
        assertThat(client.getId()).isNotBlank().isNotEqualTo(VALID_CLIENT_ID);
        assertThat(client.getGrantTypes()).contains(GrantType.client_credentials);
        oauthClientRepository.deleteById(client.getId());
    }

    @Test
    public void testUpdateOauth2ClientById() {
        val client = createOauth2Client();
        val updateOauthClientRequest = new UpdateOauthClientRequest();
        updateOauthClientRequest.setDescription(client.getDescription());
        updateOauthClientRequest.setSecret(client.getSecret());
        updateOauthClientRequest.setGrantTypes(List.of(GrantType.password));
        updateOauthClientRequest.setOrganizationId(client.getOrganizationId());
        updateOauthClientRequest.setEnabled(client.isEnabled());
        updateOauthClientRequest.setRedirectUrls(client.getRedirectUrls());
        updateOauthClientRequest.setExpiration(client.getExpiration());
        updateOauthClientRequest.setRefreshExpiration(client.getRefreshExpiration());
        updateOauthClientRequest.setTokenFormat(TokenFormat.JWT);
        val rsaKeyPair = CertificateKeysUtils.generateRsaKeyPair();
        updateOauthClientRequest.setPrivateKey(rsaKeyPair.privateKeyPem);
        updateOauthClientRequest.setPublicKey(rsaKeyPair.publicKeyPem);
        updateOauthClientRequest.setScopes(List.of());
        val updatedClient = restTestClient.post()
                .uri(API_PREFIX + "/oauth2-client/" + client.getId())
                .header("cookie", jSessionId)
                .body(updateOauthClientRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Oauth2ClientsService.OauthClientDto.class)
                .returnResult()
                .getResponseBody();
        assertThat(updatedClient).isNotNull();
        assertThat(updatedClient.getId()).isNotBlank().isEqualTo(client.getId());
        assertThat(updatedClient.getGrantTypes()).contains(GrantType.password);
        assertThat(updatedClient.getTokenFormat()).isEqualTo(TokenFormat.JWT).isNotEqualTo(client.getTokenFormat());
        oauthClientRepository.deleteById(client.getId());
    }

    @Test
    public void testDeleteClients() {
        val client = createOauth2Client();
        val deleteClientsRequest = new DeleteClientsRequest(List.of(client.getId()));
        restTestClient.method(HttpMethod.DELETE)
                .uri(API_PREFIX + "/oauth2-client")
                .header("cookie", jSessionId)
                .body(deleteClientsRequest)
                .exchange()
                .expectStatus().isOk();

        restTestClient.get()
                .uri(API_PREFIX + "/oauth2-client/" + client.getId())
                .header("cookie", jSessionId)
                .exchange()
                .expectStatus().isNotFound();
        oauthClientRepository.deleteById(client.getId());
    }

    @Test
    public void testCreateNewKeys() {
        val client = createOauth2Client();
        restTestClient.post()
                .uri(API_PREFIX + "/oauth2-client/" + client.getId() + "/create-new-keys")
                .header("cookie", jSessionId)
                .exchange()
                .expectStatus().isOk();
        oauthClientRepository.deleteById(client.getId());
    }

    @Test
    public void testAssignKeys() {
        val client = createOauth2Client();
        val rsaKeyPair = CertificateKeysUtils.generateRsaKeyPair();
        val formData = new LinkedMultiValueMap<String, String>();
        formData.add("publicKey", rsaKeyPair.publicKeyPem);
        formData.add("privateKey", rsaKeyPair.privateKeyPem);
        restTestClient.post()
                .uri(API_PREFIX + "/oauth2-client/" + client.getId() + "/assign-keys")
                .header("cookie", jSessionId)
                .body(formData)
                .exchange()
                .expectStatus().isOk();
        oauthClientRepository.deleteById(client.getId());
    }

    private Oauth2ClientsService.OauthClientDto createOauth2Client() {
        val updateOauthClientRequest = new UpdateOauthClientRequest();
        updateOauthClientRequest.setCreateTime(Instant.now());
        updateOauthClientRequest.setDescription("Test client");
        updateOauthClientRequest.setSecret(createId());
        updateOauthClientRequest.setGrantTypes(List.of(GrantType.client_credentials));
        updateOauthClientRequest.setOrganizationId(VALID_ORGANIZATION_ID);
        updateOauthClientRequest.setEnabled(true);
        updateOauthClientRequest.setRedirectUrls(List.of());
        updateOauthClientRequest.setExpiration(Duration.ofHours(1));
        updateOauthClientRequest.setRefreshExpiration(Duration.ofHours(2));
        updateOauthClientRequest.setTokenFormat(TokenFormat.STANDARD);
        updateOauthClientRequest.setLastUpdated(Instant.now());
        updateOauthClientRequest.setScopes(List.of());
        val client = restTestClient.post()
                .uri(API_PREFIX + "/oauth2-client")
                .header("cookie", jSessionId)
                .body(updateOauthClientRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Oauth2ClientsService.OauthClientDto.class)
                .returnResult()
                .getResponseBody();
        assertThat(client).isNotNull();
        return client;
    }
}