package com.authbox.base.model;

import com.authbox.base.util.DurationToMsConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;

import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;

import static jakarta.persistence.EnumType.STRING;

@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
@Builder(setterPrefix = "with")
@Entity
@Table(name = "access_log")
public class AccessLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 12149753600001L;

    @Id
    private String id;
    @Convert(converter = Jsr310JpaConverters.InstantConverter.class)
    private Instant createTime;
    private String organizationId;
    private String oauthTokenId;
    private String clientId;
    private String requestId;
    @Enumerated(STRING)
    private Source source;
    @Column(name = "duration_ms")
    @Convert(converter = DurationToMsConverter.class)
    private Duration duration;
    private String message;
    private String error;
    private int statusCode;
    private String ip;
    private String userAgent;

    public enum Source {
        Oauth2Server,
        WebManagementPortal
    }
}
