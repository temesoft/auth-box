package com.authbox.web.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@Data
public class UpdateUserRequest {

    private String id;
    private String username;
    private String password;
    private String name;
    private boolean enabled;
    private List<String> roles;

}
