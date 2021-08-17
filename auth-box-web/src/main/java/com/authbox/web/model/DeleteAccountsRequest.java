package com.authbox.web.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class DeleteAccountsRequest {

    public final List<String> accountIds;

    @JsonCreator
    public DeleteAccountsRequest(@JsonProperty("accountIds") final List<String> accountIds) {
        this.accountIds = accountIds;
    }

}
