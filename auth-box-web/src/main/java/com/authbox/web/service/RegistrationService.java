package com.authbox.web.service;

import com.authbox.base.model.User;
import com.authbox.web.model.CreateAccountWithOrganizationRequest;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

public interface RegistrationService {

    /**
     * Creates a new account with new organization using CreateAccountWithOrganizationRequest object
     */
    UserDto createAccountWithOrganization(CreateAccountWithOrganizationRequest request);

    @Builder
    @Getter
    class UserDto {
        private String id;
        private Instant createTime;
        private String username;
        private String name;
        private List<String> roles;
        private boolean enabled;
        private String organizationId;
        private Instant lastUpdated;

        public boolean isAdmin() {
            return roles.contains("ROLE_ADMIN");
        }

        static UserDto fromEntity(User entity) {
            return UserDto.builder()
                    .id(entity.getId())
                    .createTime(entity.getCreateTime())
                    .username(entity.getUsername())
                    .name(entity.getName())
                    .roles(entity.getRoles())
                    .enabled(entity.isEnabled())
                    .organizationId(entity.getOrganizationId())
                    .lastUpdated(entity.getLastUpdated())
                    .build();
        }
    }

}
