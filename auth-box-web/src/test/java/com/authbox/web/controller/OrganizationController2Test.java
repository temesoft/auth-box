package com.authbox.web.controller;

import com.authbox.web.Application;
import com.authbox.web.TestUtils;
import com.authbox.web.service.OrganizationService;
import lombok.val;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.time.Instant;
import java.util.Map;

import static com.authbox.base.util.IdUtils.createId;
import static com.authbox.web.TestConstants.VALID_ORGANIZATION_ID;
import static com.authbox.web.config.Constants.API_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = {Application.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureRestTestClient
@TestPropertySource(locations = "classpath:application-test.properties")
class OrganizationController2Test {

    private final ParameterizedTypeReference<Map<String, Object>> mapResponseType =
            new ParameterizedTypeReference<>() {
            };

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
    void testGetOrganizationDetails() {
        val organization = restTestClient.get()
                .uri(API_PREFIX + "/organization")
                .header("cookie", jSessionId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(OrganizationService.OrganizationDto.class)
                .returnResult()
                .getResponseBody();
        assertThat(organization).isNotNull();
        assertThat(organization.getId()).isEqualTo(VALID_ORGANIZATION_ID);
        assertThat(organization.getCreateTime()).isBefore(Instant.now());
        assertThat(organization.getLastUpdated()).isBefore(Instant.now());
        assertThat(organization.getName()).isEqualTo("Test organization");
        assertThat(organization.getDomainPrefix()).isEqualTo("localhost");
        assertThat(organization.getAddress()).isEqualTo("101 California St. San Francisco, CA 94107");
        assertThat(organization.isEnabled()).isTrue();
        assertThat(organization.getLogoUrl()).isNull();
    }

    @Test
    public void testCheckAvailableDomainPrefix() {
        val localhostResult = restTestClient.get()
                .uri(API_PREFIX + "/organization/available-domain-prefix/localhost")
                .header("cookie", jSessionId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(mapResponseType)
                .returnResult()
                .getResponseBody();
        assertThat(localhostResult).isNotNull().isNotEmpty();
        assertThat(localhostResult).containsEntry("exists", true);

        val unknownDomainResult = restTestClient.get()
                .uri(API_PREFIX + "/organization/available-domain-prefix/" + RandomStringUtils.secure().nextAlphabetic(10))
                .header("cookie", jSessionId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(mapResponseType)
                .returnResult()
                .getResponseBody();
        assertThat(unknownDomainResult).isNotNull().isNotEmpty();
        assertThat(unknownDomainResult).containsEntry("exists", false);
    }

    @Test
    public void testUpdate() {
        val organization = restTestClient.get()
                .uri(API_PREFIX + "/organization")
                .header("cookie", jSessionId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(OrganizationService.OrganizationDto.class)
                .returnResult()
                .getResponseBody();
        assertThat(organization).isNotNull();

        val organizationDto = OrganizationService.OrganizationDto.builder()
                .build();
        assertThat(restTestClient.post()
                .uri(API_PREFIX + "/organization")
                .header("cookie", jSessionId)
                .body(organizationDto)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(String.class)
                .returnResult().getResponseBody()
        ).contains("Invalid organization id");

        organizationDto.setId(createId());
        assertThat(restTestClient.post()
                .uri(API_PREFIX + "/organization")
                .header("cookie", jSessionId)
                .body(organizationDto)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(String.class)
                .returnResult().getResponseBody()
        ).contains("Invalid organization id");

        organizationDto.setId(VALID_ORGANIZATION_ID);
        organizationDto.setDomainPrefix("incorrect-domain-prefix");
        assertThat(restTestClient.post()
                .uri(API_PREFIX + "/organization")
                .header("cookie", jSessionId)
                .body(organizationDto)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(String.class)
                .returnResult().getResponseBody()
        ).contains("Domain prefix can only contain letters and numbers");

        organizationDto.setDomainPrefix("localhost");
        assertThat(restTestClient.post()
                .uri(API_PREFIX + "/organization")
                .header("cookie", jSessionId)
                .body(organizationDto)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(String.class)
                .returnResult().getResponseBody()
        ).contains("Organization name can not be empty");

        organizationDto.setName(organization.getName());
        organizationDto.setAddress(organization.getAddress());
        organizationDto.setEnabled(organization.isEnabled());
        val organizationUpdated = restTestClient.post()
                .uri(API_PREFIX + "/organization")
                .header("cookie", jSessionId)
                .body(organizationDto)
                .exchange()
                .expectStatus().isOk()
                .expectBody(OrganizationService.OrganizationDto.class)
                .returnResult().getResponseBody();
        assertThat(organizationUpdated).isNotNull();
    }
}