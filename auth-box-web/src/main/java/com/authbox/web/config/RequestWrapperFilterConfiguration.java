package com.authbox.web.config;

import ch.qos.logback.classic.Level;
import com.authbox.base.util.NetUtils;
import com.google.common.base.Stopwatch;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import static ch.qos.logback.classic.Level.ERROR;
import static ch.qos.logback.classic.Level.INFO;
import static ch.qos.logback.classic.Level.TRACE;
import static ch.qos.logback.classic.Level.WARN;
import static org.springframework.util.ObjectUtils.isEmpty;

@Configuration
public class RequestWrapperFilterConfiguration {

    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String REQUEST_ID_MDC_KEY = "request_id";
    public static final String REQUEST_IP_MDC_KEY = "ip";
    public static final String REQUEST_ORGANIZATION_MDC_KEY = "organization_id";

    @Bean
    @ConditionalOnProperty("request.logging.enabled")
    FilterRegistrationBean<RequestWrapperFilter> requestWrapperFilterRegistrationBean(
            @Value("${request.logging.pattern:.*}") final String uriPattern,
            @Value("${request.logging.level:INFO}") final String logLevel) {
        val registrationBean = new FilterRegistrationBean<RequestWrapperFilter>();
        val log4jMDCFilterFilter = new RequestWrapperFilter(
                Pattern.compile(uriPattern),
                Level.valueOf(logLevel)
        );
        registrationBean.setFilter(log4jMDCFilterFilter);
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registrationBean;
    }

    @AllArgsConstructor
    @Slf4j
    static class RequestWrapperFilter extends OncePerRequestFilter {

        private final Pattern uriPattern;
        private final Level level;

        @Override
        protected void doFilterInternal(final HttpServletRequest request,
                                        final HttpServletResponse response,
                                        final FilterChain chain) throws IOException, ServletException {
            val stopwatch = Stopwatch.createStarted();
            try {
                final String requestId;
                if (!isEmpty(REQUEST_ID_HEADER) && !isEmpty(request.getHeader(REQUEST_ID_HEADER))) {
                    requestId = request.getHeader(REQUEST_ID_HEADER);
                } else {
                    requestId = UUID.randomUUID().toString().replace("-", "");
                }
                MDC.put(REQUEST_ID_MDC_KEY, requestId);
                if (!isEmpty(REQUEST_ID_HEADER)) {
                    response.addHeader(REQUEST_ID_HEADER, requestId);
                }
                MDC.put(REQUEST_IP_MDC_KEY, NetUtils.getIp(request));
                chain.doFilter(request, response);
            } finally {
                if (uriPattern.matcher(request.getRequestURI()).matches()) {
                    val message = String.format(
                            "Request %s:%s responded %s, took %s",
                            request.getMethod(),
                            request.getRequestURI(),
                            response.getStatus(),
                            stopwatch.stop()
                    );
                    if (TRACE.equals(level)) {
                        log.trace(message);
                    } else if (INFO.equals(level)) {
                        log.info(message);
                    } else if (WARN.equals(level)) {
                        log.warn(message);
                    } else if (ERROR.equals(level)) {
                        log.error(message);
                    } else {
                        log.debug(message);
                    }
                }
                MDC.remove(REQUEST_ID_MDC_KEY);
                MDC.remove(REQUEST_IP_MDC_KEY);
            }
        }
    }
}
