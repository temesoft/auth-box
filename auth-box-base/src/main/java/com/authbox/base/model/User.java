package com.authbox.base.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
public class User implements UserDetails, Serializable {

    private static final long serialVersionUID = 12159753648257L;

    public final String id;
    public final Instant createTime;
    public final String username;
    @JsonIgnore
    public final String password;
    public final String name;
    public final List<String> roles;
    public final boolean enabled;
    public final String organizationId;
    public final Instant lastUpdated;

    @JsonCreator
    public User(@JsonProperty("id") final String id,
                @JsonProperty("createTime") final Instant createTime,
                @JsonProperty("username") final String username,
                @JsonProperty("password") final String password,
                @JsonProperty("name") final String name,
                @JsonProperty("roles") final List<String> roles,
                @JsonProperty("enabled") final boolean enabled,
                @JsonProperty("organizationId") final String organizationId,
                @JsonProperty("lastUpdated") final Instant lastUpdated) {
        this.id = id;
        this.createTime = createTime;
        this.username = username;
        this.password = password;
        this.name = name;
        this.roles = roles;
        this.enabled = enabled;
        this.organizationId = organizationId;
        this.lastUpdated = lastUpdated;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("createTime", createTime)
                .add("username", username)
                .add("password", password)
                .add("name", name)
                .add("roles", roles)
                .add("enabled", enabled)
                .add("organizationId", organizationId)
                .add("lastUpdated", lastUpdated)
                .toString();
    }

    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream().map((Function<String, GrantedAuthority>) SimpleGrantedAuthority::new)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    @JsonIgnore
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return enabled;
    }

    @Override
    public boolean isAccountNonLocked() {
        return enabled;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public boolean isAdmin() {
        return roles.contains("ROLE_ADMIN");
    }
}
