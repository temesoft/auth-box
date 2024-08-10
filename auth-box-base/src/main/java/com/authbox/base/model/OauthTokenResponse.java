package com.authbox.base.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

import static com.authbox.base.config.Constants.OAUTH2_ATTR_ACCESS_TOKEN;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_EXPIRES_IN;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_REFRESH_TOKEN;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_SCOPE;
import static com.authbox.base.config.Constants.OAUTH2_ATTR_TOKEN_TYPE;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
@AllArgsConstructor
@ToString
public class OauthTokenResponse implements Serializable {

    @Serial
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

}
