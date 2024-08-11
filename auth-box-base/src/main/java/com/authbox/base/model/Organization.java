package com.authbox.base.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
@Entity
@Table(name = "organization")
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class Organization implements Serializable {

    @Serial
    private static final long serialVersionUID = 12159753648251L;

    @Id
    private String id;
    @Convert(converter = Jsr310JpaConverters.InstantConverter.class)
    private Instant createTime;
    private String name;
    private String domainPrefix;
    private String address;
    private boolean enabled;
    private String logoUrl;
    @Convert(converter = Jsr310JpaConverters.InstantConverter.class)
    private Instant lastUpdated;
}
