package com.authbox.web.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@PropertySource("classpath:application.properties")
@ConfigurationProperties(prefix = "oauth2.server")
@Data
public class Oauth2ServerProperties {

    private String introspectionUri;
    private String clientId;
    private String clientSecret;
}
