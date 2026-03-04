package com.authbox.base.util;

import com.google.common.hash.Hashing;

import java.security.SecureRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Utility class for cryptographic hashing and secure random string generation.
 * <p>
 * This class provides a wrapper around Guava's {@link Hashing} for SHA-256 operations
 * and utilizes {@link SecureRandom} for generating high-entropy base32 strings.
 */
public class HashUtils {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Private constructor to prevent instantiation.
     *
     * @throws IllegalStateException if called.
     */
    private HashUtils() {
        throw new IllegalStateException("Use static methods directly, without using constructor");
    }

    /**
     * Computes the SHA-256 hash of the provided source string.
     *
     * @param source The input string to hash using UTF-8 encoding.
     * @return A hexadecimal representation of the SHA-256 hash.
     */
    public static String sha256(final String source) {
        return Hashing.sha256().hashString(source, UTF_8).toString();
    }

    /**
     * Generates a cryptographically secure random string of 64 characters using
     * the Base32 alphabet (A-Z, 2-7).
     * <p>
     * This is commonly used for generating secrets, salts, or multifactor
     * authentication (MFA) backup codes.
     *
     * @return A random 64-character Base32 string.
     */
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
