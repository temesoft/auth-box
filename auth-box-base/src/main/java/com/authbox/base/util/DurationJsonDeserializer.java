package com.authbox.base.util;

import lombok.val;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.time.Duration;
import java.util.Locale;

public class DurationJsonDeserializer extends ValueDeserializer<Duration> {

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