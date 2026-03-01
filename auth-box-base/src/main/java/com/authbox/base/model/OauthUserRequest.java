package com.authbox.base.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class OauthUserRequest {

    private String id;
    private String username;
    private String password;
    private boolean enabled;
    private String metadata;
    private boolean using2Fa;

}
