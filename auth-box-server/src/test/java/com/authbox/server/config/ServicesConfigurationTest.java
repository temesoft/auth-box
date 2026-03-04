package com.authbox.server.config;

import com.authbox.base.model.AccessLog;
import com.authbox.base.service.AccessLogService;
import com.authbox.server.Application;
import com.authbox.server.service.ParsingValidationService;
import com.authbox.server.service.ScopeService;
import com.authbox.server.service.TokenDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = {Application.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureRestTestClient
@TestPropertySource(locations = "classpath:application-test.properties")
public class ServicesConfigurationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    public void testCorrectDefaultClockUTC() {
        assertThat(context.getBeansOfType(Clock.class))
                .isNotEmpty()
                .hasSize(1)
                .containsEntry("defaultClock", Clock.systemUTC());
    }

    @Test
    public void testRegisteredServices() {
        assertThat(context.getBeansOfType(Clock.class)).isNotEmpty().hasSize(1);
        assertThat(context.getBeansOfType(AccessLog.Source.class)).isNotEmpty().hasSize(1);
        assertThat(context.getBeansOfType(TokenDetailsService.class)).isNotEmpty().hasSize(1);
        assertThat(context.getBeansOfType(ScopeService.class)).isNotEmpty().hasSize(1);
        assertThat(context.getBeansOfType(ParsingValidationService.class)).isNotEmpty().hasSize(1);
        assertThat(context.getBeansOfType(AccessLogService.class)).isNotEmpty().hasSize(1);
    }
}