package com.authbox.web.service;

import com.authbox.base.model.OauthUser;
import com.authbox.base.model.Organization;
import com.authbox.base.model.UpdateOauthUserRequest;
import com.authbox.web.model.DeleteUsersRequest;
import com.authbox.web.model.PasswordChangeRequest;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.awt.image.BufferedImage;
import java.time.Instant;

public interface Oauth2UsersService {

    /**
     * Returns paginated list of OauthUserDto objects for provided organization
     */
    Page<OauthUserDto> getOauth2Users(Organization organization, int pageSize, int currentPage);

    /**
     * Returns OauthUserDto object for provided organization by id
     */
    OauthUserDto getOauth2UserById(Organization organization, String id);

    /**
     * Update password for specified user with provided organization using PasswordChangeRequest object
     */
    OauthUserDto updatePassword(Organization organization, String userId, PasswordChangeRequest passwordChangeRequest);

    /**
     * Generates QR code for 2FA for user with provided organization
     */
    BufferedImage generate2FaQrCodeImage(Organization organization, String userId);

    /**
     * Updates user with provided organization using UpdateOauthUserRequest object
     */
    OauthUserDto updateOauth2UserById(Organization organization, String userId, UpdateOauthUserRequest updatedOauthUser);

    /**
     * Creates a new user for provided organization using UpdateOauthUserRequest object
     */
    OauthUserDto createOauth2User(Organization organization, UpdateOauthUserRequest newOauthUser);

    /**
     * Deletes user for provided organization using DeleteUsersRequest object
     */
    void deleteUsers(Organization organization, DeleteUsersRequest deleteUsersRequest);

    @Builder
    @Getter
    class OauthUserDto {
        private String id;
        private Instant createTime;
        private String username;
        private boolean enabled;
        private String organizationId;
        private String metadata;
        private boolean using2Fa;
        private Instant lastUpdated;

        static OauthUserDto fromEntity(OauthUser entity) {
            return OauthUserDto.builder()
                    .id(entity.getId())
                    .createTime(entity.getCreateTime())
                    .username(entity.getUsername())
                    .enabled(entity.isEnabled())
                    .organizationId(entity.getOrganizationId())
                    .metadata(entity.getMetadata())
                    .using2Fa(entity.isUsing2Fa())
                    .lastUpdated(entity.getLastUpdated())
                    .build();
        }
    }
}
