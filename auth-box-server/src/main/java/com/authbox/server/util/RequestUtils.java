package com.authbox.server.util;

import com.authbox.server.filter.RequestWrapperFilter;
import org.slf4j.MDC;

import java.time.Duration;

import static com.authbox.server.filter.RequestWrapperFilter.REQUEST_ID_MDC_KEY;
import static com.authbox.server.filter.RequestWrapperFilter.REQUEST_START_REQUEST_TIME_MDC_KEY;

/**
 * Utility class for accessing request-scoped metadata stored in the Mapped Diagnostic Context (MDC).
 * <p>
 * This class facilitates retrieving tracking information such as request IDs and execution
 * durations that are typically populated by the {@link RequestWrapperFilter} during
 * the lifecycle of an HTTP request.
 */
public class RequestUtils {

    /**
     * Private constructor to prevent instantiation.
     *
     * @throws IllegalStateException if called.
     */
    private RequestUtils() {
        throw new IllegalStateException("Use static methods directly, without using constructor");
    }

    /**
     * Calculates the duration elapsed since the current request began.
     * <p>
     * This method relies on a timestamp stored in the MDC under the
     * {@link RequestWrapperFilter#REQUEST_START_REQUEST_TIME_MDC_KEY} key.
     *
     * @return A {@link Duration} representing the time passed since the request start.
     * @throws NumberFormatException if the MDC value is missing or not a valid long.
     */
    public static Duration getTimeSinceRequest() {
        return Duration.ofMillis(System.currentTimeMillis()
                - Long.parseLong(MDC.get(REQUEST_START_REQUEST_TIME_MDC_KEY)));
    }

    /**
     * Retrieves the unique identifier for the current request from the MDC.
     *
     * @return The request ID string stored under {@link RequestWrapperFilter#REQUEST_ID_MDC_KEY},
     * or {@code null} if no ID is present.
     */
    public static String getRequestId() {
        return MDC.get(REQUEST_ID_MDC_KEY);
    }
}
