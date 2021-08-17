package com.authbox.web.config;

import com.authbox.base.util.NetUtils;
import org.slf4j.MDC;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

import static org.springframework.util.ObjectUtils.isEmpty;

@Configuration
public class RequestWrapperFilterConfiguration {

    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String REQUEST_ID_MDC_KEY = "request_id";
    public static final String REQUEST_IP_MDC_KEY = "ip";
    public static final String REQUEST_ORGANIZATION_MDC_KEY = "organization_id";

    @Bean
    public FilterRegistrationBean<RequestWrapperFilter> requestWrapperFilterRegistrationBean() {
        final FilterRegistrationBean<RequestWrapperFilter> registrationBean = new FilterRegistrationBean<>();
        final RequestWrapperFilter log4jMDCFilterFilter = new RequestWrapperFilter();
        registrationBean.setFilter(log4jMDCFilterFilter);
        registrationBean.setOrder(2);
        return registrationBean;
    }

    static class RequestWrapperFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) throws java.io.IOException, ServletException {
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
                MDC.remove(REQUEST_ID_MDC_KEY);
                MDC.remove(REQUEST_IP_MDC_KEY);
            }
        }
    }

}
