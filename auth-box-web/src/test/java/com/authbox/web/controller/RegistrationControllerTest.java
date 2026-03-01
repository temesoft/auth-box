package com.authbox.web.controller;

import com.authbox.base.dao.OrganizationRepository;
import com.authbox.base.dao.UserRepository;
import com.authbox.web.Application;
import com.authbox.web.model.CreateAccountWithOrganizationRequest;
import com.authbox.web.model.UserDto;
import lombok.val;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = {Application.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureRestTestClient
@TestPropertySource(locations = "classpath:application-test.properties")
class RegistrationControllerTest {

    @Autowired
    private RestTestClient restTestClient;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private OrganizationRepository organizationRepository;

    @Test
    void testCreateAccountWithOrganization() {
        val request = new CreateAccountWithOrganizationRequest(
                null,
                "test-" + RandomStringUtils.secure().nextAlphabetic(10),
                "password",
                "password",
                "Tester " + RandomStringUtils.secure().nextAlphabetic(10),
                "Organization " + RandomStringUtils.secure().nextAlphabetic(10),
                RandomStringUtils.secure().nextAlphabetic(10),
                null
        );

        val userDto = restTestClient.post()
                .uri("/registration")
                .body(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(UserDto.class)
                .returnResult().getResponseBody();
        assertThat(userDto).isNotNull();

        assertThat(userRepository.findByUsername(userDto.getUsername())).isPresent();
        assertThat(organizationRepository.findById(userDto.getOrganizationId())).isPresent();

        userRepository.deleteById(userDto.getId());
        organizationRepository.deleteById(userDto.getOrganizationId());
    }
}