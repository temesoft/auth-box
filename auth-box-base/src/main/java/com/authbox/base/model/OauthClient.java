package com.authbox.base.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.MoreObjects;

import java.io.IOException;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
public class OauthClient implements Serializable {

    private static final long serialVersionUID = 12159753648252L;

    public final String id;
    public final Instant createTime;
    public final String description;
    public final String secret;
    public final List<GrantType> grantTypes;
    public final String organizationId;
    public final boolean enabled;
    public final List<String> redirectUrls;
    public final Duration expiration;
    public final Duration refreshExpiration;
    public final TokenFormat tokenFormat;
    @JsonIgnore
    public final String privateKey;
    public final String publicKey;
    public final Instant lastUpdated;


    // Non-null, set at runtime, used for json DTO
    private List<OauthScope> scopes;
    // Non-null, set at runtime, used for json DTO
    private List<String> scopeIds;

    @JsonCreator
    public OauthClient(@JsonProperty("id") final String id,
                       @JsonProperty("createTime") final Instant createTime,
                       @JsonProperty("description") final String description,
                       @JsonProperty("secret") final String secret,
                       @JsonProperty("grantTypes") final List<GrantType> grantTypes,
                       @JsonProperty("organizationId") final String organizationId,
                       @JsonProperty("enabled") final boolean enabled,
                       @JsonProperty("redirectUrls") final List<String> redirectUrls,
                       @JsonDeserialize(using = DurationJsonDeserializer.class)
                       @JsonSerialize(using = DurationJsonSerializer.class)
                       @JsonProperty("expiration") final Duration expiration,
                       @JsonDeserialize(using = DurationJsonDeserializer.class)
                       @JsonSerialize(using = DurationJsonSerializer.class)
                       @JsonProperty("refreshExpiration") final Duration refreshExpiration,
                       @JsonProperty("tokenFormat") final TokenFormat tokenFormat,
                       @JsonProperty("privateKey") final String privateKey,
                       @JsonProperty("publicKey") final String publicKey,
                       @JsonProperty("lastUpdated") final Instant lastUpdated) {
        this.id = id;
        this.createTime = createTime;
        this.description = description;
        this.secret = secret;
        this.grantTypes = grantTypes;
        this.organizationId = organizationId;
        this.enabled = enabled;
        this.redirectUrls = redirectUrls;
        this.expiration = expiration;
        this.refreshExpiration = refreshExpiration;
        this.tokenFormat = tokenFormat;
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.lastUpdated = lastUpdated;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("createTime", createTime)
                .add("description", description)
                .add("grantTypes", grantTypes)
                .add("organizationId", organizationId)
                .add("enabled", enabled)
                .add("scopes", scopes)
                .add("redirectUrls", redirectUrls)
                .add("expiration", expiration)
                .add("refreshExpiration", refreshExpiration)
                .add("tokenFormat", tokenFormat)
                .add("lastUpdated", lastUpdated)
                .add("privateKey", privateKey.length() + " bytes")
                .add("publicKey", publicKey.length() + " bytes")
                .toString();
    }

    public List<OauthScope> getScopes() {
        return scopes;
    }

    public void setScopes(final List<OauthScope> scopes) {
        this.scopes = scopes;
    }


    public List<String> getScopeIds() {
        return scopeIds;
    }

    public void setScopeIds(final List<String> scopeIds) {
        this.scopeIds = scopeIds;
    }


    public OauthClient withScopeIds(final List<String> scopeIds) {
        this.scopeIds = scopeIds;
        return this;
    }


    public OauthClient withScopes(final List<OauthScope> scopes) {
        this.scopes = scopes;
        return this;
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
