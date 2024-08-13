package com.authbox.base.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Duration;

public class DurationJsonDeserializer extends JsonDeserializer<Duration> {
    @Override
    public Duration deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext) throws IOException {
        return Duration.parse("PT" + jsonParser.getValueAsString());
    }
}