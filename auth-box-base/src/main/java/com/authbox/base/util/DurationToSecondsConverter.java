package com.authbox.base.util;

import javax.persistence.AttributeConverter;
import java.time.Duration;

public class DurationToSecondsConverter implements AttributeConverter<Duration, Long> {

    @Override
    public Long convertToDatabaseColumn(final Duration duration) {
        if (duration == null) return null;
        return duration.toSeconds();
    }

    @Override
    public Duration convertToEntityAttribute(final Long seconds) {
        if (seconds == null) return Duration.ZERO;
        return Duration.ofSeconds(seconds);
    }
}
