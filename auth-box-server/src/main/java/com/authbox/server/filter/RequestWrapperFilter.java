package com.authbox.server.filter;

import com.authbox.base.model.AccessLog;
import com.authbox.base.service.AccessLogService;
import com.authbox.base.util.NetUtils;
import com.google.common.base.Stopwatch;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import static com.authbox.base.config.Constants.OAUTH_PREFIX;
import static com.authbox.base.util.HashUtils.makeRequestId;
import static org.apache.commons.lang3.StringUtils.isEmpty;

@AllArgsConstructor
@Slf4j
public class RequestWrapperFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String REQUEST_ID_MDC_KEY = "request_id";
    public static final String REQUEST_IP_MDC_KEY = "ip";
    public static final String REQUEST_START_REQUEST_TIME_MDC_KEY = "timestamp";
    public static final String REQUEST_DOMAIN_PREFIX_MDC_KEY = "domain_prefix";
    public static final String REQUEST_ORGANIZATION_MDC_KEY = "organization_id";

    private final AccessLogService accessLogService;

    @Override
    protected void doFilterInternal(final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final FilterChain chain) {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        final boolean oauthCall = request.getRequestURI().startsWith(OAUTH_PREFIX);
        MDC.put(REQUEST_START_REQUEST_TIME_MDC_KEY, String.valueOf(System.currentTimeMillis()));
        try {
            final String requestId;
            if (!isEmpty(request.getHeader(REQUEST_ID_HEADER)) && request.getHeader(REQUEST_ID_HEADER).length() <= 36) {
                requestId = request.getHeader(REQUEST_ID_HEADER);
            } else {
                requestId = makeRequestId();
            }
            if (oauthCall) {
                accessLogService.create(
                        AccessLog.builder()
                                .withIp(NetUtils.getIp(request))
                                .withUserAgent(NetUtils.getUserAgent(request))
                                .withRequestId(requestId),
                        "Request started: " + request.getMethod() + " " + request.getRequestURL()
                );
            }

            MDC.put(REQUEST_ID_MDC_KEY, requestId);
            if (!isEmpty(REQUEST_ID_HEADER)) {
                response.addHeader(REQUEST_ID_HEADER, requestId);
            }
            MDC.put(REQUEST_IP_MDC_KEY, request.getRemoteAddr());
            chain.doFilter(request, response);
        } catch (Exception e) {
            if (oauthCall) {
                accessLogService.create(
                        AccessLog.builder()
                                .withRequestId(MDC.get(REQUEST_ID_MDC_KEY))
                                .withDuration(stopwatch.elapsed())
                                .withStatusCode(response.getStatus())
                                .withError("internal error"),
                        e.getMessage()
                );
            }
            log.error("Error during request processing", e);
        } finally {
            if (oauthCall) {
                accessLogService.create(
                        AccessLog.builder()
                                .withRequestId(MDC.get(REQUEST_ID_MDC_KEY))
                                .withStatusCode(response.getStatus())
                                .withDuration(stopwatch.stop().elapsed()),
                        "Request finished"
                );
            }

            accessLogService.processCachedAccessLogs();

            MDC.remove(REQUEST_ID_MDC_KEY);
            MDC.remove(REQUEST_IP_MDC_KEY);
        }
    }
}
