package com.authbox.base.model;

import com.google.common.base.MoreObjects;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

public class OauthToken implements Serializable {

    private static final long serialVersionUID = 12159753648255L;

    public final String id;
    public final Instant createTime;
    public final String hash;
    public final String organizationId;
    public final String clientId;
    public final Instant expiration;
    public final List<String> scopes;
    public final String oauthUserId;
    public final TokenType tokenType;
    public final String ip;
    public final String userAgent;
    public final String requestId;
    public String linkedTokenId;

    public OauthToken(final String id, final Instant createTime, final String hash, final String organizationId, final String clientId, final Instant expiration, final List<String> scopes, final String oauthUserId, final TokenType tokenType, final String ip, final String userAgent, final String requestId) {
        this.id = id;
        this.createTime = createTime;
        this.hash = hash;
        this.organizationId = organizationId;
        this.clientId = clientId;
        this.expiration = expiration;
        this.scopes = scopes;
        this.oauthUserId = oauthUserId;
        this.tokenType = tokenType;
        this.ip = ip;
        this.userAgent = userAgent;
        this.requestId = requestId;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("createTime", createTime)
                .add("organizationId", organizationId)
                .add("clientId", clientId)
                .add("expiration", expiration)
                .add("scopes", scopes)
                .add("oauthUserId", oauthUserId)
                .add("tokenType", tokenType)
                .add("ip", ip)
                .add("userAgent", userAgent)
                .add("requestId", requestId)
                .toString();
    }

    public String getLinkedTokenId() {
        return linkedTokenId;
    }

    public OauthToken withLinkedTokenId(final String linkedTokenId) {
        this.linkedTokenId = linkedTokenId;
        return this;
    }
}
