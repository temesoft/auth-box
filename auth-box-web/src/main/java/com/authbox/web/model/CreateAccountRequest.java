package com.authbox.web.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateAccountRequest {

    public final String id;
    public final String username;
    @JsonIgnore
    public final String password;
    public final String name;
    public final UserRole role;

    @JsonCreator
    public CreateAccountRequest(@JsonProperty("id") final String id,
                                @JsonProperty("username") final String username,
                                @JsonProperty("password") final String password,
                                @JsonProperty("name") final String name,
                                @JsonProperty("role") final UserRole role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.name = name;
        this.role = role;
    }
}
