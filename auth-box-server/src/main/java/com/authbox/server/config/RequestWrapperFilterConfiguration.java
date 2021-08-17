package com.authbox.server.config;

import com.authbox.base.service.AccessLogService;
import com.authbox.base.util.NetUtils;
import com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.authbox.base.model.AccessLog.AccessLogBuilder.accessLogBuilder;
import static com.authbox.base.util.HashUtils.makeRequestId;
import static org.apache.commons.lang3.StringUtils.isEmpty;

@Configuration
public class RequestWrapperFilterConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestWrapperFilterConfiguration.class);

    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String REQUEST_ID_MDC_KEY = "request_id";
    public static final String REQUEST_IP_MDC_KEY = "ip";
    public static final String REQUEST_START_REQUEST_TIME_MDC_KEY = "timestamp";
    public static final String REQUEST_DOMAIN_PREFIX_MDC_KEY = "domain_prefix";
    public static final String REQUEST_ORGANIZATION_MDC_KEY = "organization_id";

    @Bean
    public FilterRegistrationBean<RequestWrapperFilter> requestWrapperFilterRegistrationBean(final AccessLogService accessLogService) {
        final FilterRegistrationBean<RequestWrapperFilter> registrationBean = new FilterRegistrationBean<>();
        final RequestWrapperFilter log4jMDCFilterFilter = new RequestWrapperFilter(accessLogService);
        registrationBean.setFilter(log4jMDCFilterFilter);
        registrationBean.setOrder(2);
        return registrationBean;
    }

    static class RequestWrapperFilter extends OncePerRequestFilter {

        private final AccessLogService accessLogService;

        public RequestWrapperFilter(final AccessLogService accessLogService) {
            this.accessLogService = accessLogService;
        }

        @Override
        protected void doFilterInternal(final HttpServletRequest request,
                                        final HttpServletResponse response,
                                        final FilterChain chain) {
            final Stopwatch stopwatch = Stopwatch.createStarted();
//            accessLogService.initCachedAccessLogs();
            MDC.put(REQUEST_START_REQUEST_TIME_MDC_KEY, String.valueOf(System.currentTimeMillis()));
            try {
                final String requestId;
                if (!isEmpty(REQUEST_ID_HEADER) && !isEmpty(request.getHeader(REQUEST_ID_HEADER)) && request.getHeader(REQUEST_ID_HEADER).length() <= 36) {
                    requestId = request.getHeader(REQUEST_ID_HEADER);
                } else {
                    requestId = makeRequestId();
                }
                accessLogService.create(
                        accessLogBuilder()
                                .withIp(NetUtils.getIp(request))
                                .withUserAgent(NetUtils.getUserAgent(request))
                                .withRequestId(requestId),
                        "Request started: " + request.getMethod() + " " + request.getRequestURL()
                );

                MDC.put(REQUEST_ID_MDC_KEY, requestId);
                if (!isEmpty(REQUEST_ID_HEADER)) {
                    response.addHeader(REQUEST_ID_HEADER, requestId);
                }
                MDC.put(REQUEST_IP_MDC_KEY, request.getRemoteAddr());
                chain.doFilter(request, response);
            } catch (Exception e) {
                accessLogService.create(
                        accessLogBuilder()
                                .withRequestId(MDC.get(REQUEST_ID_MDC_KEY))
                                .withDuration(stopwatch.elapsed())
                                .withStatusCode(response.getStatus())
                                .withError("internal error"),
                        e.getMessage()
                );
                LOGGER.error("Error during request processing", e);
            } finally {
                accessLogService.create(
                        accessLogBuilder()
                                .withRequestId(MDC.get(REQUEST_ID_MDC_KEY))
                                .withStatusCode(response.getStatus())
                                .withDuration(stopwatch.stop().elapsed()),
                        "Request finished"
                );

                accessLogService.processCachedAccessLogs();

                MDC.remove(REQUEST_ID_MDC_KEY);
                MDC.remove(REQUEST_IP_MDC_KEY);
            }
        }
    }
}
