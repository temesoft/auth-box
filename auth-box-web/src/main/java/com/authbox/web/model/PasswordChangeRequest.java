package com.authbox.web.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PasswordChangeRequest {

    public final String oldPassword;
    public final String newPassword;
    public final String newPassword2;

    @JsonCreator
    public PasswordChangeRequest(@JsonProperty("oldPassword") final String oldPassword,
                                 @JsonProperty("newPassword") final String newPassword,
                                 @JsonProperty("newPassword2") final String newPassword2) {
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
        this.newPassword2 = newPassword2;
    }
}
