package com.authbox.base.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
@Entity
@Table(name = "oauth_scope")
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class OauthScope implements Comparable<OauthScope>, Serializable {

    @Serial
    private static final long serialVersionUID = 12159753648254L;

    @Id
    private String id;
    @Convert(converter = Jsr310JpaConverters.InstantConverter.class)
    private Instant createTime;
    private String description;
    private String scope;
    private String organizationId;

    @Override
    public int compareTo(final OauthScope oauthScope2) {
        return this.scope.compareTo(oauthScope2.scope);
    }
}
