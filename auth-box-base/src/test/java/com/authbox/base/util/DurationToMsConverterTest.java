package com.authbox.base.util;

import lombok.val;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class DurationToMsConverterTest {

    private final DurationToMsConverter converter = new DurationToMsConverter();

    @Test
    void testConvertToDatabaseColumnWithValidDuration() {
        val duration = Duration.ofSeconds(5);
        val result = converter.convertToDatabaseColumn(duration);
        assertThat(result).isEqualTo(5000L);
    }

    @Test
    void testConvertToDatabaseColumnWithNull() {
        val result = converter.convertToDatabaseColumn(null);
        assertThat(result).isNull();
    }

    @Test
    void testConvertToEntityAttributeWithValidMillis() {
        val millis = 2500L;
        val result = converter.convertToEntityAttribute(millis);
        assertThat(result).isEqualTo(Duration.ofMillis(2500));
    }

    @Test
    void testConvertToEntityAttributeWithNull() {
        val result = converter.convertToEntityAttribute(null);
        assertThat(result).isEqualTo(Duration.ZERO);
    }

    @Test
    void testConvertToEntityAttributeWithZero() {
        val result = converter.convertToEntityAttribute(0L);
        assertThat(result).isEqualTo(Duration.ZERO);
    }
}