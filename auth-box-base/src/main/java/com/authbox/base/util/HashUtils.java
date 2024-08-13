package com.authbox.base.util;

import com.google.common.hash.Hashing;

import java.security.SecureRandom;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.nio.charset.StandardCharsets.UTF_8;

public class HashUtils {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private HashUtils() {
    }

    public static String sha256(final String source) {
        return Hashing.sha256().hashString(source, UTF_8).toString();
    }

    public static String makeRandomBase32() {
        final String result;
        final var characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        final var charactersLength = characters.length();
        result = IntStream.range(0, 64)
                .mapToObj(i -> String.valueOf(characters.charAt((int) Math.floor(SECURE_RANDOM.nextDouble() * charactersLength))))
                .collect(Collectors.joining());
        return result;
    }
}
