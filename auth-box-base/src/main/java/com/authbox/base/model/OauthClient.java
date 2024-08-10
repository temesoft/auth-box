package com.authbox.base.model;

import com.authbox.base.util.DurationToSecondsConverter;
import com.authbox.base.util.GrantTypeConverter;
import com.authbox.base.util.ListOfStringsConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.EmptyInterceptor;
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static javax.persistence.EnumType.STRING;

@JsonInclude(NON_NULL)
@Entity
@Table(name = "oauth_client")
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class OauthClient extends EmptyInterceptor implements Serializable {

    @Serial
    private static final long serialVersionUID = 12159753648252L;

    @Id
    private String id;

    @Convert(converter = Jsr310JpaConverters.InstantConverter.class)
    private Instant createTime;

    private String description;

    private String secret;

    @Convert(converter = GrantTypeConverter.class)
    @Column(name = "grant_types_csv")
    private List<GrantType> grantTypes;

    private String organizationId;

    private boolean enabled;

    @Convert(converter = ListOfStringsConverter.class)
    @Column(name = "redirect_urls_csv")
    private List<String> redirectUrls;

    @Convert(converter = DurationToSecondsConverter.class)
    @JsonDeserialize(using = DurationJsonDeserializer.class)
    @JsonSerialize(using = DurationJsonSerializer.class)
    @Column(name = "expiration_seconds")
    private Duration expiration;

    @Convert(converter = DurationToSecondsConverter.class)
    @JsonDeserialize(using = DurationJsonDeserializer.class)
    @JsonSerialize(using = DurationJsonSerializer.class)
    @Column(name = "refresh_expiration_seconds")
    private Duration refreshExpiration;

    @Enumerated(STRING)
    private TokenFormat tokenFormat;

    @JsonIgnore
    @ToString.Exclude
    private String privateKey;

    @ToString.Exclude
    private String publicKey;

    @Convert(converter = Jsr310JpaConverters.InstantConverter.class)
    private Instant lastUpdated;

    @ManyToMany
    @JoinTable(
            name = "oauth_client_scope",
            joinColumns = @JoinColumn(name = "client_id"),
            inverseJoinColumns = @JoinColumn(name = "scope_id"))
    private List<OauthScope> scopes;

    @Transient
    private List<String> scopeIds;

    public List<String> getScopeIds() {
        if (scopeIds != null) {
            return scopeIds;
        } else if (scopes != null) {
            return scopes.stream().map(OauthScope::getId).toList();
        } else {
            return null;
        }
    }

    static class DurationJsonSerializer extends JsonSerializer<Duration> {
        @Override
        public void serialize(final Duration duration, final JsonGenerator jsonGenerator, final SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeString(duration.toString().replaceAll("PT", "").toLowerCase());
        }
    }

    static class DurationJsonDeserializer extends JsonDeserializer<Duration> {
        @Override
        public Duration deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            return Duration.parse("PT" + jsonParser.getValueAsString());
        }
    }
}
