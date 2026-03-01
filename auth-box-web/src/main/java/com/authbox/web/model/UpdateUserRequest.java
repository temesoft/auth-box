package com.authbox.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class UpdateUserRequest {

    private String id;
    private String username;
    private String password;
    private String name;
    private boolean enabled;
    private List<String> roles;

}
