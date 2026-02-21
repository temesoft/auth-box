package com.authbox.base.util;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class DurationToSecondsConverterTest {

    private DurationToSecondsConverter converter;

    @BeforeEach
    void setUp() {
        converter = new DurationToSecondsConverter();
    }

    @Test
    void testConvertToDatabaseColumnWithDuration() {
        val duration = Duration.ofMinutes(2);
        val result = converter.convertToDatabaseColumn(duration);
        assertThat(result).isEqualTo(120L);
    }

    @Test
    void testConvertToDatabaseColumnWithNull() {
        val result = converter.convertToDatabaseColumn(null);
        assertThat(result).isNull();
    }

    @Test
    void testConvertToEntityAttributeWithSeconds() {
        val seconds = 300L;
        val result = converter.convertToEntityAttribute(seconds);
        assertThat(result).isEqualTo(Duration.ofMinutes(5));
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