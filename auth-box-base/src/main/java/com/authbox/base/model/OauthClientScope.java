package com.authbox.base.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
@Entity
@Table(name = "oauth_client_scope")
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class OauthClientScope implements Serializable {

    @Serial
    private static final long serialVersionUID = 12159753648253L;

    @Id
    private String id;
    @Convert(converter = Jsr310JpaConverters.InstantConverter.class)
    private Instant createTime;
    @Column(name = "client_id")
    private String clientId;
    @Column(name = "scope_id")
    private String scopeId;
}
