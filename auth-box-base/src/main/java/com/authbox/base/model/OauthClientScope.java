package com.authbox.base.model;

import com.google.common.base.MoreObjects;

import java.io.Serializable;
import java.time.Instant;

public class OauthClientScope implements Serializable {

    private static final long serialVersionUID = 12159753648253L;

    public final String id;
    public final Instant createTime;
    public final String clientId;
    public final String scopeId;

    public OauthClientScope(final String id, final Instant createTime, final String clientId, final String scopeId) {
        this.id = id;
        this.createTime = createTime;
        this.clientId = clientId;
        this.scopeId = scopeId;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("createTime", createTime)
                .add("clientId", clientId)
                .add("scopeId", scopeId)
                .toString();
    }
}
