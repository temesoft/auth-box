package com.authbox.base.util;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DurationJsonSerializerTest {

    private DurationJsonSerializer serializer;
    private JsonGenerator jsonGenerator;
    private SerializationContext serializationContext;

    @BeforeEach
    void setUp() {
        serializer = new DurationJsonSerializer();
        jsonGenerator = mock(JsonGenerator.class);
        serializationContext = mock(SerializationContext.class);
    }

    @Test
    void testSerializeWhenDurationIsNull() {
        serializer.serialize(null, jsonGenerator, serializationContext);
        verify(jsonGenerator).writeNull();
    }

    @Test
    void testSerializeWhenDurationIsPresent() {
        val duration = Duration.ofMinutes(5).plusSeconds(30);
        val captor = ArgumentCaptor.forClass(String.class);
        serializer.serialize(duration, jsonGenerator, serializationContext);
        verify(jsonGenerator).writeString(captor.capture());
        assertThat(captor.getValue()).isEqualTo("5m30s");
    }

    @Test
    void testSerializeWhenDurationIsHours() {
        val duration = Duration.ofHours(2);
        val captor = ArgumentCaptor.forClass(String.class);
        serializer.serialize(duration, jsonGenerator, serializationContext);
        verify(jsonGenerator).writeString(captor.capture());
        assertThat(captor.getValue()).isEqualTo("2h");
    }
}