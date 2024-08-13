package com.authbox.base.model;

import com.authbox.base.util.DurationJsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class UpdateOauthClientRequest {

    private Instant createTime;
    private String description;
    private String secret;
    private List<GrantType> grantTypes;
    private String organizationId;
    private boolean enabled;
    private List<String> redirectUrls;
    @JsonDeserialize(using = DurationJsonDeserializer.class)
    private Duration expiration;
    @JsonDeserialize(using = DurationJsonDeserializer.class)
    private Duration refreshExpiration;
    private TokenFormat tokenFormat;
    private String privateKey;
    private String publicKey;
    private Instant lastUpdated;
    private List<OauthScope> scopes;
    private List<String> scopeIds;

}
