package com.authbox.base.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class UpdateOauthUserRequest {

    private String id;
    private String username;
    private String password;
    private boolean enabled;
    private String metadata;
    private boolean using2Fa;

}
