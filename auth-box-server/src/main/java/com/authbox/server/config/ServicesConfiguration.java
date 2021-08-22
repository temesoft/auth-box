package com.authbox.server.config;

import com.authbox.base.config.AppProperties;
import com.authbox.base.config.DaoConfiguration;
import com.authbox.base.config.ExceptionHandlerConfiguration;
import com.authbox.base.config.StartupTasksConfiguration;
import com.authbox.base.dao.AccessLogDao;
import com.authbox.base.dao.OauthClientDao;
import com.authbox.base.dao.OauthTokenDao;
import com.authbox.base.dao.OauthUserDao;
import com.authbox.base.model.AccessLog;
import com.authbox.base.service.AccessLogService;
import com.authbox.base.service.AccessLogServiceImpl;
import com.authbox.base.service.AccessLogThreadCache;
import com.authbox.server.service.ParsingValidationService;
import com.authbox.server.service.ParsingValidationServiceImpl;
import com.authbox.server.service.ScopeService;
import com.authbox.server.service.ScopeServiceImpl;
import com.authbox.server.service.TokenDetailsService;
import com.authbox.server.service.TokenDetailsServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.time.Clock;

@Configuration
@Import({
        DaoConfiguration.class,
        StartupTasksConfiguration.class,
        ExceptionHandlerConfiguration.class,
        AppProperties.class
})
public class ServicesConfiguration {

    @Bean
    AccessLog.Source source() {
        return AccessLog.Source.Oauth2Server;
    }

    @Bean
    Clock defaultClock() {
        return Clock.systemUTC();
    }

    @Bean
    TokenDetailsService tokenDetailsService(final OauthTokenDao oauthTokenDao,
                                            final OauthUserDao oauthUserDao,
                                            final OauthClientDao oauthClientDao,
                                            final Clock defaultClock,
                                            final ObjectMapper objectMapper,
                                            final AccessLogService accessLogService) {
        return new TokenDetailsServiceImpl(oauthTokenDao, oauthUserDao, oauthClientDao, defaultClock, objectMapper, accessLogService);
    }

    @Bean
    ScopeService scopeService(final AccessLogService accessLogService) {
        return new ScopeServiceImpl(accessLogService);
    }

    @Bean
    ParsingValidationService parsingValidationService(final OauthClientDao oauthClientDao, final AccessLogService accessLogService) {
        return new ParsingValidationServiceImpl(oauthClientDao, accessLogService);
    }

    @Bean
    AccessLogService accessLogService(final Clock defaultClock,
                                      final AccessLog.Source source,
                                      final AccessLogDao accessLogDao) {
        return new AccessLogServiceImpl(defaultClock, source, accessLogDao, new AccessLogThreadCache());
    }
}
