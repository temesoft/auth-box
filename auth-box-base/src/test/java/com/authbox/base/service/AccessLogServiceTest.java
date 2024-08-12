package com.authbox.base.service;

import com.authbox.base.config.AppProperties;
import com.authbox.base.dao.AccessLogDao;
import com.authbox.base.model.AccessLog;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.val;
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
        val logDao = mock(AccessLogDao.class);
        val accessLogThreadCache = new AccessLogThreadCache();
        val service = new AccessLogServiceImpl(
                new AppProperties(),
                new SimpleMeterRegistry(),
                Clock.systemUTC(),
                Oauth2Server,
                logDao,
                accessLogThreadCache
        );
        service.create(AccessLog.builder(), "Test message");
        service.processCachedAccessLogs();
        await().atMost(Duration.ofSeconds(2)).until(() -> service.getQueue().isEmpty());
        verify(logDao, times(1)).insert(any());
    }
}