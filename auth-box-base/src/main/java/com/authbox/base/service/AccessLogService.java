package com.authbox.base.service;

import com.authbox.base.model.AccessLog;
import com.authbox.base.model.Organization;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.Duration;
import java.time.Instant;

public interface AccessLogService {

    void create(AccessLog.AccessLogBuilder builder, String message, String... arguments);

    void processCachedAccessLogs();

    Page<AccessLogDto> getAccessLogByRequestId(Organization organization, String requestId);

    @Builder
    @Getter
    class AccessLogDto {
        private String id;
        private Instant createTime;
        private String organizationId;
        private String oauthTokenId;
        private String clientId;
        private String requestId;
        private com.authbox.base.model.AccessLog.Source source;
        private Duration duration;
        private String message;
        private String error;
        private int statusCode;
        private String ip;
        private String userAgent;

        public static AccessLogDto fromEntity(final AccessLog accessLog) {
            return AccessLogDto.builder()
                    .id(accessLog.getId())
                    .createTime(accessLog.getCreateTime())
                    .organizationId(accessLog.getOrganizationId())
                    .oauthTokenId(accessLog.getOauthTokenId())
                    .clientId(accessLog.getClientId())
                    .requestId(accessLog.getRequestId())
                    .source(accessLog.getSource())
                    .duration(accessLog.getDuration())
                    .message(accessLog.getMessage())
                    .error(accessLog.getError())
                    .statusCode(accessLog.getStatusCode())
                    .ip(accessLog.getIp())
                    .userAgent(accessLog.getUserAgent())
                    .build();
        }
    }

}
