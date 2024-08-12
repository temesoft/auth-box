package com.authbox.base.model;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum GrantType {

    client_credentials,
    password,
    authorization_code,
    refresh_token;

    private static final List<String> all = List.copyOf(
            Arrays.stream(GrantType.values())
                    .map(Enum::name)
                    .collect(Collectors.toList())
    );

    public static boolean contains(final String value) {
        return all.contains(value);
    }
}