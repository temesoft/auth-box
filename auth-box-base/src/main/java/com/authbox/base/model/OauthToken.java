package com.authbox.base.model;

import com.authbox.base.util.ListOfStringsConverter;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static javax.persistence.EnumType.STRING;

@JsonInclude(NON_NULL)
@Entity
@Table(name = "oauth_token")
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class OauthToken implements Serializable {

    private static final long serialVersionUID = 12159753648255L;

    @Id
    private String id;
    @Convert(converter = Jsr310JpaConverters.InstantConverter.class)
    private Instant createTime;
    @ToString.Exclude
    private String hash;
    private String organizationId;
    private String clientId;
    @Convert(converter = Jsr310JpaConverters.InstantConverter.class)
    private Instant expiration;
    @Convert(converter = ListOfStringsConverter.class)
    @Column(name = "scopes_csv")
    private List<String> scopes;
    private String oauthUserId;
    @Enumerated(STRING)
    private TokenType tokenType;
    private String ip;
    private String userAgent;
    private String requestId;
    private String linkedTokenId;

}
