package com.authbox.web.service;

import com.authbox.base.model.Organization;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;

public interface OrganizationService {

    /**
     * Returns a map for verification of domain presence for provided organization
     * Example: {"exists": true}
     */
    Map<String, Object> checkAvailableDomainPrefix(Organization organization, String domainPrefix);

    /**
     * Updates organization using provided organization and updated OrganizationDto object
     */
    OrganizationDto update(Organization organization, OrganizationDto updatedOrganization);

    /**
     * Validates a domain prefix for provided organization id
     */
    void validateDomainPrefix(String domainPrefix, String providedOrganizationId);

    @Builder
    @Getter
    class OrganizationDto {
        private String id;
        private Instant createTime;
        private String name;
        private String domainPrefix;
        private String address;
        private boolean enabled;
        private String logoUrl;
        @Setter
        private Instant lastUpdated;

        public static OrganizationDto fromEntity(Organization entity) {
            return OrganizationDto.builder()
                    .id(entity.getId())
                    .createTime(entity.getCreateTime())
                    .name(entity.getName())
                    .domainPrefix(entity.getDomainPrefix())
                    .address(entity.getAddress())
                    .enabled(entity.isEnabled())
                    .logoUrl(entity.getLogoUrl())
                    .lastUpdated(entity.getLastUpdated())
                    .build();
        }
    }

}
