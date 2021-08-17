package com.authbox.base.util;

import com.google.common.hash.Hashing;

import java.security.SecureRandom;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

public class HashUtils {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private HashUtils() {
    }

    public static String sha256(final String source) {
        return Hashing.sha256().hashString(source, UTF_8).toString();
    }

    public static String makeRandomBase32() {
        final var result = new StringBuilder();
        final var characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        final var charactersLength = characters.length();
        for (var i = 0; i < 64; i++) {
            result.append(characters.charAt((int) Math.floor(SECURE_RANDOM.nextDouble() * charactersLength)));
        }
        return result.toString();
    }

    public static String makeRequestId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
