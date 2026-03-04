package com.authbox.base.util;

import com.github.ksuid.KsuidGenerator;

import java.security.SecureRandom;

/**
 * Utility class for generating cryptographically secure KSUIDs (K-Sortable Unique Identifiers).
 * <p>
 * This class provides a centralized generator using {@link SecureRandom} to ensure
 * high entropy and collision resistance. KSUIDs are naturally sortable by their
 * generation timestamp while maintaining uniqueness.
 * @see
 */
public class IdUtils {

    private static final KsuidGenerator GENERATOR = new KsuidGenerator(new SecureRandom());

    /**
     * Private constructor to prevent instantiation.
     *
     * @throws IllegalStateException if called.
     */
    private IdUtils() {
        throw new IllegalStateException("Use static methods directly, without using constructor");
    }

    /**
     * Generates a new KSUID and returns its string representation.
     *
     * @return A 27-character base-62 encoded KSUID string.
     */
    public static String createId() {
        return GENERATOR.newKsuid().toString();
    }
}
