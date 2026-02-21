package com.authbox.base.config;

import com.authbox.base.SpringBootBaseTest;
import com.authbox.base.dao.AccessLogDao;
import com.authbox.base.model.AccessLog;
import com.authbox.base.service.AccessLogService;
import com.authbox.base.service.AccessLogServiceImpl;
import com.authbox.base.service.AccessLogThreadCache;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.net.UnknownHostException;
import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        classes = {
                DataSourceAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class,
                DaoConfiguration.class,
                StartupTasksConfigurationTest.TestConfig.class,
                StartupTasksConfiguration.class
        },
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class StartupTasksConfigurationTest extends SpringBootBaseTest {

    @MockitoSpyBean
    private AccessLogService accessLogService;
    @Autowired
    private StartupTasksConfiguration startupTasksConfiguration;

    @Test
    public void testStartupTasksConfiguration() throws UnknownHostException {
        startupTasksConfiguration.preDestroyTasks();
        val userCaptor = ArgumentCaptor.forClass(AccessLog.AccessLogBuilder.class);
        verify(accessLogService, times(2)).create(userCaptor.capture(), any());
        val values = userCaptor.getAllValues();
        assertThat(values).hasSize(2);
        val message1 = values.get(0).build();
        assertThat(message1.getMessage()).contains("Oauth2Server startup on ip=");
        val message2 = values.get(1).build();
        assertThat(message2.getMessage()).contains("Oauth2Server shutdown on ip=");
    }

    @Configuration
    static class TestConfig {
        @Bean
        AccessLog.Source mockSource() {
            return AccessLog.Source.Oauth2Server;
        }

        @Bean
        @Primary
        AccessLogService mockAccessLogService(final AccessLogDao accessLogDao,
                                              final AccessLog.Source source) {
            return new AccessLogServiceImpl(
                    new AppProperties(),
                    new SimpleMeterRegistry(),
                    Clock.systemUTC(),
                    source,
                    accessLogDao,
                    new AccessLogThreadCache()
            );
        }
    }
}