package com.authbox.web.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class DeleteTokensRequest {

    public final List<String> tokenIds;

    @JsonCreator
    public DeleteTokensRequest(@JsonProperty("tokenIds") final List<String> tokenIds) {
        this.tokenIds = tokenIds;
    }
}
