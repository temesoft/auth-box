package com.authbox.web.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class DeleteUsersRequest {

    public final List<String> userIds;

    @JsonCreator
    public DeleteUsersRequest(@JsonProperty("userIds") final List<String> userIds) {
        this.userIds = userIds;
    }
}
