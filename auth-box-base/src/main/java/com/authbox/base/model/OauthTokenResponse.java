package com.authbox.base.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import java.io.Serializable;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.authbox.base.config.Constants.*;

@JsonInclude(NON_NULL)
public class OauthTokenResponse implements Serializable {

    private static final long serialVersionUID = 12159753648256L;

    @JsonProperty(OAUTH2_ATTR_ACCESS_TOKEN)
    public final String accessToken;
    @JsonProperty(OAUTH2_ATTR_TOKEN_TYPE)
    public final String tokenType;
    @JsonProperty(OAUTH2_ATTR_EXPIRES_IN)
    public final long expiresIn;
    @JsonProperty(OAUTH2_ATTR_REFRESH_TOKEN)
    public final String refreshToken;
    @JsonProperty(OAUTH2_ATTR_SCOPE)
    public final String scope;

    public OauthTokenResponse(final String accessToken, final String tokenType, final long expiresIn, final String refreshToken, final String scope) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.refreshToken = refreshToken;
        this.scope = scope;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("accessToken", accessToken)
                .add("tokenType", tokenType)
                .add("expiresIn", expiresIn)
                .add("refreshToken", refreshToken)
                .add("scope", scope)
                .toString();
    }
}
