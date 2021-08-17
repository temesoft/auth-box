package com.authbox.base.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import java.io.Serializable;
import java.time.Instant;

public class OauthUser implements Serializable {

    private static final long serialVersionUID = 12159753648257L;

    public final String id;
    public final Instant createTime;
    public final String username;
    @JsonIgnore
    public final String password;
    public final boolean enabled;
    public final String organizationId;
    public final String metadata;
    public final boolean using2Fa;
    @JsonIgnore
    public final String secret;
    public final Instant lastUpdated;

    @JsonCreator
    public OauthUser(@JsonProperty("id") final String id,
                     @JsonProperty("createTime") final Instant createTime,
                     @JsonProperty("username") final String username,
                     @JsonProperty("password") final String password,
                     @JsonProperty("enabled") final boolean enabled,
                     @JsonProperty("organizationId") final String organizationId,
                     @JsonProperty("metadata") final String metadata,
                     @JsonProperty("using2Fa") final boolean using2Fa,
                     @JsonProperty("secret") final String secretBase32,
                     @JsonProperty("lastUpdated") final Instant lastUpdated) {
        this.id = id;
        this.createTime = createTime;
        this.username = username;
        this.password = password;
        this.enabled = enabled;
        this.organizationId = organizationId;
        this.metadata = metadata;
        this.using2Fa = using2Fa;
        this.secret = secretBase32;
        this.lastUpdated = lastUpdated;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("createTime", createTime)
                .add("username", username)
                .add("enabled", enabled)
                .add("organizationId", organizationId)
                .add("metadata", metadata)
                .add("using2Fa", using2Fa)
                .add("secret", secret)
                .add("lastUpdated", lastUpdated)
                .toString();
    }
}
