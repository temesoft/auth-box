package com.authbox.base.util;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class DurationJsonDeserializerTest {

    private DurationJsonDeserializer deserializer;
    private JsonParser jsonParser;
    private DeserializationContext context;

    @BeforeEach
    void setUp() {
        deserializer = new DurationJsonDeserializer();
        jsonParser = Mockito.mock(JsonParser.class);
        context = Mockito.mock(DeserializationContext.class);
    }

    @ParameterizedTest
    @CsvSource({
            "P1D, PT24H",
            "PT1H, PT1H",
            "30M, PT30M",
            "5S, PT5S"
    })
    void testDeserialize_ValidInputs(final String input, final String expectedIso) {
        when(jsonParser.getValueAsString()).thenReturn(input);
        val result = deserializer.deserialize(jsonParser, context);
        assertThat(result).isEqualTo(Duration.parse(expectedIso));
    }

    @Test
    void testDeserialize_InvalidInputs() {
        when(jsonParser.getValueAsString()).thenReturn(null);
        assertThat(deserializer.deserialize(jsonParser, context)).isNull();
        when(jsonParser.getValueAsString()).thenReturn("   ");
        assertThat(deserializer.deserialize(jsonParser, context)).isNull();
    }
}