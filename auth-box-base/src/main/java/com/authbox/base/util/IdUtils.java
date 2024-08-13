package com.authbox.base.util;

import com.github.ksuid.KsuidGenerator;

import java.security.SecureRandom;

public class IdUtils {

    private static final KsuidGenerator GENERATOR = new KsuidGenerator(new SecureRandom());

    private IdUtils() {
    }

    public static String createId() {
        return GENERATOR.newKsuid().toString();
    }
}
