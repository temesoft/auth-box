package com.authbox.base.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@PropertySource("classpath:application.properties")
@ConfigurationProperties(prefix = "info.app")
public class AppProperties {

    private String name;
    private String description;
    private String version;
    private String domain = "";
    private String protocol = "https";
    private int port = 0;
    private boolean registrationEnabled = true;
    private boolean allowTokenDetailsWithoutClientCredentials = false;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(final String domain) {
        this.domain = domain;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(final String protocol) {
        this.protocol = protocol;
    }

    public int getPort() {
        return port;
    }

    public void setPort(final int port) {
        this.port = port;
    }

    public boolean isRegistrationEnabled() {
        return registrationEnabled;
    }

    public void setRegistrationEnabled(final boolean registrationEnabled) {
        this.registrationEnabled = registrationEnabled;
    }

    public boolean isAllowTokenDetailsWithoutClientCredentials() {
        return allowTokenDetailsWithoutClientCredentials;
    }

    public void setAllowTokenDetailsWithoutClientCredentials(final boolean allowTokenDetailsWithoutClientCredentials) {
        this.allowTokenDetailsWithoutClientCredentials = allowTokenDetailsWithoutClientCredentials;
    }
}
