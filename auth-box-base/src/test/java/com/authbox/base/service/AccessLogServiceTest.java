package com.authbox.base.service;

import com.authbox.base.dao.AccessLogDao;
import com.authbox.base.model.AccessLog;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;

import static com.authbox.base.model.AccessLog.Source.Oauth2Server;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class AccessLogServiceTest {

    @Test
    public void testAccessLogQueueProcessing() {
        final AccessLogDao logDao = mock(AccessLogDao.class);
        final AccessLogThreadCache accessLogThreadCache = new AccessLogThreadCache();
        final AccessLogServiceImpl service = new AccessLogServiceImpl(Clock.systemUTC(), Oauth2Server, logDao, accessLogThreadCache);
        service.create(AccessLog.builder(), "Test message");
        service.processCachedAccessLogs();
        await().atMost(Duration.ofSeconds(2)).until(() -> service.getQueue().isEmpty());
        verify(logDao, times(1)).insert(any());
    }
}