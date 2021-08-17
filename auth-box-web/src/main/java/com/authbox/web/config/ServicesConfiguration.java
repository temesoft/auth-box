package com.authbox.web.config;

import com.authbox.base.config.DaoConfiguration;
import com.authbox.base.config.ExceptionHandlerConfiguration;
import com.authbox.base.config.StartupTasksConfiguration;
import com.authbox.base.dao.AccessLogDao;
import com.authbox.base.model.AccessLog;
import com.authbox.base.service.AccessLogService;
import com.authbox.base.service.AccessLogServiceImpl;
import com.authbox.base.service.AccessLogThreadCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;

@Configuration
@Import({
        DaoConfiguration.class,
        ExceptionHandlerConfiguration.class,
        StartupTasksConfiguration.class
})
public class ServicesConfiguration {

    @Bean
    AccessLog.Source source() {
        return AccessLog.Source.WebManagementPortal;
    }

    @Bean
    Clock defaultClock() {
        return Clock.systemUTC();
    }

    @Bean
    AccessLogService accessLogService(final Clock defaultClock, final AccessLog.Source source, final AccessLogDao accessLogDao) {
        return new AccessLogServiceImpl(defaultClock, source, accessLogDao, new AccessLogThreadCache());
    }

    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
