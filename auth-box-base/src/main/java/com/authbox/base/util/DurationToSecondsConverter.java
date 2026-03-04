package com.authbox.base.util;

import jakarta.persistence.AttributeConverter;

import java.time.Duration;

/**
 * JPA Attribute Converter to persist {@link Duration} objects as seconds in the database.
 * <p>
 * This converter maps a Java {@link Duration} to a {@link Long} database column. It is
 * useful for storing time intervals in a numeric format that is easily queryable
 * and platform-independent.
 */
public class DurationToSecondsConverter implements AttributeConverter<Duration, Long> {

    /**
     * Converts a {@link Duration} to its total seconds for database storage.
     *
     * @param duration The entity attribute value to be converted.
     * @return The total seconds as a {@link Long}, or {@code null} if the input is null.
     */
    @Override
    public Long convertToDatabaseColumn(final Duration duration) {
        if (duration == null) return null;
        return duration.toSeconds();
    }

    /**
     * Converts a long value representing seconds from the database back into a {@link Duration}.
     * <p>
     * If the database value is {@code null}, this implementation returns {@link Duration#ZERO}.
     *
     * @param seconds The database column value to be converted.
     * @return A {@link Duration} instance, or {@link Duration#ZERO} if the input is null.
     */
    @Override
    public Duration convertToEntityAttribute(final Long seconds) {
        if (seconds == null) return Duration.ZERO;
        return Duration.ofSeconds(seconds);
    }
}
