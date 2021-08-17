package com.authbox.base.model;

import com.google.common.base.MoreObjects;

import java.io.Serializable;
import java.time.Instant;

public class OauthScope implements Comparable<OauthScope>, Serializable {

    private static final long serialVersionUID = 12159753648254L;

    public final String id;
    public final Instant createTime;
    public final String description;
    public final String scope;
    public final String organizationId;

    public OauthScope(final String id, final Instant createTime, final String description, final String scope, final String organizationId) {
        this.id = id;
        this.createTime = createTime;
        this.description = description;
        this.scope = scope;
        this.organizationId = organizationId;
    }

    @Override
    public int compareTo(final OauthScope oauthScope2) {
        return this.scope.compareTo(oauthScope2.scope);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("createTime", createTime)
                .add("description", description)
                .add("scope", scope)
                .add("organizationId", organizationId)
                .toString();
    }
}
