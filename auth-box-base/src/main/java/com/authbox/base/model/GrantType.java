package com.authbox.base.model;

import com.google.common.collect.ImmutableList;

import java.util.List;

public enum GrantType {

    client_credentials,
    password,
    authorization_code,
    refresh_token;

    private static final List<String> all = ImmutableList.of("client_credentials", "password", "authorization_code", "refresh_token");

    public static boolean contains(final String value) {
        return all.contains(value);
    }
}