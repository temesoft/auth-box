package com.authbox.base.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "oauth_user")
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class OauthUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 12159753648257L;

    @Id
    private String id;
    @Convert(converter = Jsr310JpaConverters.InstantConverter.class)
    private Instant createTime;
    private String username;
    @JsonIgnore
    @ToString.Exclude
    private String password;
    private boolean enabled;
    private String organizationId;
    private String metadata;
    @Column(name = "using_2fa")
    private boolean using2Fa;
    @JsonIgnore
    @ToString.Exclude
    private String secret;
    @Convert(converter = Jsr310JpaConverters.InstantConverter.class)
    private Instant lastUpdated;
}
