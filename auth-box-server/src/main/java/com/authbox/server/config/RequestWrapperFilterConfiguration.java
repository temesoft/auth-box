package com.authbox.server.config;

import com.authbox.base.service.AccessLogService;
import com.authbox.server.filter.RequestWrapperFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class RequestWrapperFilterConfiguration {

    @Bean
    FilterRegistrationBean<RequestWrapperFilter> requestWrapperFilterRegistrationBean(final AccessLogService accessLogService) {
        final FilterRegistrationBean<RequestWrapperFilter> registrationBean = new FilterRegistrationBean<>();
        final RequestWrapperFilter log4jMDCFilterFilter = new RequestWrapperFilter(accessLogService);
        registrationBean.setFilter(log4jMDCFilterFilter);
        registrationBean.setOrder(2);
        return registrationBean;
    }
}
