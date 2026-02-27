package com.authbox.web.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ScopesRequest {

    public final List<String> scopeIds;

    @JsonCreator
    public ScopesRequest(@JsonProperty("scopeIds") final List<String> scopeIds) {
        this.scopeIds = scopeIds;
    }
}
