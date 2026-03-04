package com.authbox.base.util;

import lombok.val;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.time.Duration;
import java.util.Locale;

/**
 * Custom Jackson deserializer for {@link Duration} objects.
 * <p>
 * This deserializer supports both standard ISO-8601 duration formats (e.g., "PT1H")
 * and simplified numeric-based strings by automatically prefixing "PT" if the
 * leading "P" designator is missing.
 */
public class DurationJsonDeserializer extends ValueDeserializer<Duration> {

    /**
     * Deserializes a JSON string into a {@link Duration} instance.
     * <p>
     * If the input string does not start with 'P' (case-insensitive), it is treated
     * as a time-based duration and prefixed with "PT" before parsing.
     *
     * @param jsonParser             The underlying JSON parser.
     * @param deserializationContext Context for the process.
     * @return A {@link Duration} instance, or {@code null} if the value is null or blank.
     */
    @Override
    public Duration deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext) {
        val value = jsonParser.getValueAsString();
        if (value == null || value.isBlank()) {
            return null;
        }
        val isoValue = value.toUpperCase(Locale.ROOT).startsWith("P") ? value : "PT" + value;
        return Duration.parse(isoValue);
    }
}