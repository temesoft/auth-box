package com.authbox.base.util;

import jakarta.persistence.AttributeConverter;
import java.time.Duration;

public class DurationToMsConverter implements AttributeConverter<Duration, Long> {

    @Override
    public Long convertToDatabaseColumn(final Duration duration) {
        if (duration == null) return null;
        return duration.toMillis();
    }

    @Override
    public Duration convertToEntityAttribute(final Long seconds) {
        if (seconds == null) return Duration.ZERO;
        return Duration.ofMillis(seconds);
    }
}
