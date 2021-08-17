package com.authbox.web.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class DeleteClientsRequest {

    public final List<String> clientIds;

    @JsonCreator
    public DeleteClientsRequest(@JsonProperty("clientIds") final List<String> clientIds) {
        this.clientIds = clientIds;
    }

}
