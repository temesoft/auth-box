package com.authbox.web.model;

import com.authbox.base.model.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.List;

@Builder
@Getter
public class UserDto {
    private String id;
    private Instant createTime;
    private String username;
    @JsonIgnore
    @ToString.Exclude
    private String password;
    private String name;
    private List<String> roles;
    private boolean enabled;
    private String organizationId;
    private Instant lastUpdated;

    public boolean isAdmin() {
        return roles.contains("ROLE_ADMIN");
    }

    public static UserDto fromEntity(User entity) {
        return UserDto.builder()
                .id(entity.getId())
                .createTime(entity.getCreateTime())
                .username(entity.getUsername())
                .password(entity.getPassword())
                .name(entity.getName())
                .roles(entity.getRoles())
                .enabled(entity.isEnabled())
                .organizationId(entity.getOrganizationId())
                .lastUpdated(entity.getLastUpdated())
                .build();
    }
}
