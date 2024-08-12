package com.authbox.server.util;

import org.slf4j.MDC;

import java.time.Duration;

import static com.authbox.server.filter.RequestWrapperFilter.REQUEST_ID_MDC_KEY;
import static com.authbox.server.filter.RequestWrapperFilter.REQUEST_START_REQUEST_TIME_MDC_KEY;

public class RequestUtils {

    public static Duration getTimeSinceRequest() {
        return Duration.ofMillis(System.currentTimeMillis() - Long.parseLong(MDC.get(REQUEST_START_REQUEST_TIME_MDC_KEY)));
    }

    public static String getRequestId() {
        return MDC.get(REQUEST_ID_MDC_KEY);
    }
}
