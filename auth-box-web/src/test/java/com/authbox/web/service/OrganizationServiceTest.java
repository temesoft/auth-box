package com.authbox.web.service;

import com.authbox.base.model.Organization;
import lombok.val;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static com.authbox.base.util.IdUtils.createId;
import static org.assertj.core.api.Assertions.assertThat;

class OrganizationServiceTest {

    @Test
    public void testFromEntity() {
        val organization = Organization.builder()
                .withId(createId())
                .withCreateTime(Instant.now())
                .withLastUpdated(Instant.now())
                .withName(RandomStringUtils.secure().nextAlphabetic(10))
                .withDomainPrefix(RandomStringUtils.secure().nextAlphabetic(10))
                .withAddress(RandomStringUtils.secure().nextAlphabetic(10))
                .withLogoUrl(RandomStringUtils.secure().nextAlphabetic(10))
                .build();
        val organizationDto = OrganizationService.OrganizationDto.fromEntity(organization);
        assertThat(organizationDto).isNotNull();
        assertThat(organizationDto.getId()).isEqualTo(organization.getId());
        assertThat(organizationDto.getName()).isEqualTo(organization.getName());
        assertThat(organizationDto.getDomainPrefix()).isEqualTo(organization.getDomainPrefix());
    }

}