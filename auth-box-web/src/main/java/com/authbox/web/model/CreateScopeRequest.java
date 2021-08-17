package com.authbox.web.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateScopeRequest {

    public final String scope;
    public final String description;

    @JsonCreator
    public CreateScopeRequest(@JsonProperty("scope") final String scope,
                              @JsonProperty("description") final String description) {
        this.scope = scope;
        this.description = description;
    }
}
