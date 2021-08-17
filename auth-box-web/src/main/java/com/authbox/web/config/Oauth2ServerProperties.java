package com.authbox.web.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@PropertySource("classpath:application.properties")
@ConfigurationProperties(prefix = "oauth2.server")
public class Oauth2ServerProperties {

    private String introspectionUri;
    private String clientId;
    private String clientSecret;

    public String getIntrospectionUri() {
        return introspectionUri;
    }

    public void setIntrospectionUri(final String introspectionUri) {
        this.introspectionUri = introspectionUri;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(final String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(final String clientSecret) {
        this.clientSecret = clientSecret;
    }
}
