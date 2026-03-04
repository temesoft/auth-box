package com.authbox.base.util;


import lombok.val;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.time.Duration;
import java.util.Locale;

/**
 * Custom Jackson serializer for {@link Duration} objects.
 * <p>
 * This serializer converts {@link Duration} instances into a simplified string format
 * by removing the ISO-8601 "PT" prefix and converting the remaining duration
 * string to lowercase (e.g., "PT1H30M" becomes "1h30m").
 */
public class DurationJsonSerializer extends ValueSerializer<Duration> {

    /**
     * Serializes a {@link Duration} instance into a simplified, lowercase string.
     * <p>
     * If the duration is null, a null literal is written to the JSON output.
     *
     * @param duration      The Duration value to serialize.
     * @param jsonGenerator The generator used to output the JSON content.
     * @param ctx           The serialization context.
     */
    @Override
    public void serialize(final Duration duration, final JsonGenerator jsonGenerator, final SerializationContext ctx) {
        if (duration == null) {
            jsonGenerator.writeNull();
            return;
        }
        val formatted = duration.toString().replace("PT", "").toLowerCase(Locale.ROOT);
        jsonGenerator.writeString(formatted);
    }
}