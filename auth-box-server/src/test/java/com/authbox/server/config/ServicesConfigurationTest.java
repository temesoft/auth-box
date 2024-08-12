package com.authbox.server.config;

import com.authbox.server.SpringContextTest;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

public class ServicesConfigurationTest extends SpringContextTest {

    @Test
    public void testCorrectDefaultClockUTC() {
        val servicesConfiguration = new ServicesConfiguration();
        assertThat(servicesConfiguration.defaultClock()).isEqualTo(Clock.systemUTC());
    }
}