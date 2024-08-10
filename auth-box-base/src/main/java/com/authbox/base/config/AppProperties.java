package com.authbox.base.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@PropertySource("classpath:application.properties")
@ConfigurationProperties(prefix = "info.app")
@Getter
@Setter
public class AppProperties {

    private String name;
    private String description;
    private String version;
    private String domain = "";
    private String protocol = "https";
    private Duration accessQueueProcessingIdle = Duration.ofSeconds(5);
    private Duration accessQueueProcessingPull = Duration.ofSeconds(1);
    private int port = 0;
    private boolean registrationEnabled = true;
    private boolean allowTokenDetailsWithoutClientCredentials = true;
}
