package com.authbox.server.util;

import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static com.authbox.server.filter.RequestWrapperFilter.REQUEST_ID_MDC_KEY;
import static com.authbox.server.filter.RequestWrapperFilter.REQUEST_START_REQUEST_TIME_MDC_KEY;
import static org.assertj.core.api.Assertions.assertThat;

class RequestUtilsTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void testGetRequestIdReturnsValueFromMdc() {
        val expectedId = "test-request-id";
        MDC.put(REQUEST_ID_MDC_KEY, expectedId);
        val actualId = RequestUtils.getRequestId();
        assertThat(actualId).isEqualTo(expectedId);
    }

    @Test
    void testGetRequestIdReturnsNullWhenEmpty() {
        val actualId = RequestUtils.getRequestId();
        assertThat(actualId).isNull();
    }

    @Test
    void testGetTimeSinceRequestCalculatesCorrectDuration() {
        val startTime = System.currentTimeMillis() - 1000;
        MDC.put(REQUEST_START_REQUEST_TIME_MDC_KEY, String.valueOf(startTime));
        val duration = RequestUtils.getTimeSinceRequest();
        assertThat(duration.toMillis()).isGreaterThanOrEqualTo(1000);
    }
}