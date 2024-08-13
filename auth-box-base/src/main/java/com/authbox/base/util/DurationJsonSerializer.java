package com.authbox.base.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.Duration;

public class DurationJsonSerializer extends JsonSerializer<Duration> {
    @Override
    public void serialize(final Duration duration, final JsonGenerator jsonGenerator, final SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeString(duration.toString().replaceAll("PT", "").toLowerCase());
    }
}