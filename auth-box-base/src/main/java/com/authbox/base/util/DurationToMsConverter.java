package com.authbox.base.util;

import jakarta.persistence.AttributeConverter;

import java.time.Duration;

/**
 * JPA Attribute Converter to persist {@link Duration} objects as milliseconds in the database.
 * <p>
 * This converter maps a Java {@link Duration} to a {@link Long} database column,
 * providing a standardized way to store time intervals in relational databases
 * that may not have a native duration type.
 */
public class DurationToMsConverter implements AttributeConverter<Duration, Long> {

    /**
     * Converts a {@link Duration} to its millisecond representation for database storage.
     *
     * @param duration The entity attribute value to be converted.
     * @return The duration in milliseconds, or {@code null} if the input is null.
     */
    @Override
    public Long convertToDatabaseColumn(final Duration duration) {
        if (duration == null) return null;
        return duration.toMillis();
    }

    /**
     * Converts a millisecond value from the database back into a {@link Duration}.
     * <p>
     * Note: If the database value is {@code null}, this implementation returns
     * {@link Duration#ZERO} rather than {@code null}.
     *
     * @param seconds The database column value (in milliseconds) to be converted.
     * @return A {@link Duration} instance, or {@link Duration#ZERO} if the input is null.
     */
    @Override
    public Duration convertToEntityAttribute(final Long seconds) {
        if (seconds == null) return Duration.ZERO;
        return Duration.ofMillis(seconds);
    }
}
