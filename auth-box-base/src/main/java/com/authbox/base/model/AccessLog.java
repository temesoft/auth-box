package com.authbox.base.model;

import com.google.common.base.MoreObjects;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;

public class AccessLog implements Serializable {

    private static final long serialVersionUID = 12149753600001L;

    public final String id;
    public final Instant createTime;
    public final String organizationId;
    public final String oauthTokenId;
    public final String clientId;
    public final String requestId;
    public final Source source;
    public final Duration duration;
    public final String message;
    public final String error;
    public final int statusCode;
    public final String ip;
    public final String userAgent;

    /**
     * User builder below {@link AccessLogBuilder#accessLogBuilder()}
     */
    private AccessLog(final String id,
                      final Instant createTime,
                      final String organizationId,
                      final String oauthTokenId,
                      final String clientId,
                      final String requestId,
                      final Source source,
                      final long durationMs,
                      final String message,
                      final String error,
                      final int statusCode,
                      final String ip,
                      final String userAgent) {
        this.id = id;
        this.createTime = createTime;
        this.organizationId = organizationId;
        this.oauthTokenId = oauthTokenId;
        this.clientId = clientId;
        this.requestId = requestId;
        this.source = source;
        this.duration = Duration.ofMillis(durationMs);
        this.message = message;
        this.error = error;
        this.statusCode = statusCode;
        this.ip = ip;
        this.userAgent = userAgent;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("createTime", createTime)
                .add("organizationId", organizationId)
                .add("oauthTokenId", oauthTokenId)
                .add("clientId", clientId)
                .add("requestId", requestId)
                .add("source", source)
                .add("duration", duration)
                .add("message", message)
                .add("error", error)
                .add("statusCode", statusCode)
                .add("ip", ip)
                .add("userAgent", userAgent)
                .toString();
    }

    public enum Source {
        Oauth2Server,
        WebManagementPortal
    }

    public static final class AccessLogBuilder {
        private String organizationId;
        private String oauthTokenId;
        private String clientId;
        private String requestId;
        private Duration duration;
        private String error;
        private int statusCode;
        private String ip;
        private String userAgent;

        private AccessLogBuilder() {
        }

        public static AccessLogBuilder accessLogBuilder() {
            return new AccessLogBuilder();
        }

        public AccessLogBuilder withOrganizationId(final String organizationId) {
            this.organizationId = organizationId;
            return this;
        }

        public AccessLogBuilder withOauthTokenId(final String oauthTokenId) {
            this.oauthTokenId = oauthTokenId;
            return this;
        }

        public AccessLogBuilder withClientId(final String clientId) {
            this.clientId = clientId;
            return this;
        }

        public AccessLogBuilder withRequestId(final String requestId) {
            this.requestId = requestId;
            return this;
        }

        public AccessLogBuilder withDuration(final Duration duration) {
            this.duration = duration;
            return this;
        }

        public AccessLogBuilder withError(final String error) {
            this.error = error;
            return this;
        }

        public AccessLogBuilder withStatusCode(final int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public AccessLogBuilder withIp(final String ip) {
            this.ip = ip;
            return this;
        }

        public AccessLogBuilder withUserAgent(final String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public AccessLog build(final String id, final Instant createTime, final Source source, final String message) {
            return new AccessLog(
                    id,
                    createTime,
                    organizationId,
                    oauthTokenId,
                    clientId,
                    requestId,
                    source,
                    duration != null ? duration.toMillis() : 0,
                    message,
                    error,
                    statusCode,
                    ip,
                    userAgent);
        }
    }
}
