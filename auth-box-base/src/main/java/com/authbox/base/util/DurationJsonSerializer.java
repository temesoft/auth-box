package com.authbox.base.util;


import lombok.val;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.time.Duration;
import java.util.Locale;

public class DurationJsonSerializer extends ValueSerializer<Duration> {

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