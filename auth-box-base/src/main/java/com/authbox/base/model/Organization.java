package com.authbox.base.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import java.io.Serializable;
import java.time.Instant;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
public class Organization implements Serializable {

    private static final long serialVersionUID = 12159753648251L;

    public final String id;
    public final Instant createTime;
    public final String name;
    public final String domainPrefix;
    public final String address;
    public final boolean enabled;
    public final Instant lastUpdated;

    @JsonCreator
    public Organization(@JsonProperty("id") final String id,
                        @JsonProperty("createTime") final Instant createTime,
                        @JsonProperty("name") final String name,
                        @JsonProperty("domainPrefix") final String domainPrefix,
                        @JsonProperty("address") final String address,
                        @JsonProperty("enabled") final boolean enabled,
                        @JsonProperty("lastUpdated") final Instant lastUpdated) {
        this.id = id;
        this.createTime = createTime;
        this.name = name;
        this.domainPrefix = domainPrefix;
        this.address = address;
        this.enabled = enabled;
        this.lastUpdated = lastUpdated;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("createTime", createTime)
                .add("name", name)
                .add("domainPrefix", domainPrefix)
                .add("address", address)
                .add("enabled", enabled)
                .add("lastUpdated", lastUpdated)
                .toString();
    }
}
