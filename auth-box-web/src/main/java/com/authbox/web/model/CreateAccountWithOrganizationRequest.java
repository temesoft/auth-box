package com.authbox.web.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateAccountWithOrganizationRequest {

    public final String id;
    public final String username;
    @JsonIgnore
    public final String password;
    @JsonIgnore
    public final String password2;
    public final String name;
    public final String organizationName;
    public final String domainPrefix;

    public CreateAccountWithOrganizationRequest(
            @JsonProperty("id") final String id,
            @JsonProperty("username") final String username,
            @JsonProperty("password") final String password,
            @JsonProperty("password2") final String password2,
            @JsonProperty("name") final String name,
            @JsonProperty("organizationName") final String organizationName,
            @JsonProperty("domainPrefix") final String domainPrefix) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.password2 = password2;
        this.name = name;
        this.organizationName = organizationName;
        this.domainPrefix = domainPrefix;
    }
}
