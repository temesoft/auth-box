package com.authbox.base.service;

import com.authbox.base.config.AppProperties;
import com.authbox.base.dao.AccessLogDao;
import com.authbox.base.model.AccessLog;
import com.authbox.base.model.Organization;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.Uninterruptibles;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import static com.authbox.base.config.Constants.METRIC_KEY_ACCESS_LOG_SERVICE_QUEUE;
import static com.authbox.base.dao.AccessLogDaoImpl.LIST_CRITERIA_ORGANIZATION_ID;
import static com.authbox.base.dao.AccessLogDaoImpl.LIST_CRITERIA_REQUEST_ID;
import static com.authbox.base.util.IdUtils.createId;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.commons.lang3.ArrayUtils.addFirst;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;

@Slf4j
public class AccessLogServiceImpl implements AccessLogService, DisposableBean {

    private static final int MAX_BATCH_SIZE = 100;
    private static final int MAX_QUEUE_SIZE = 10_000;
    private final BlockingQueue<AccessLog> QUEUE = new LinkedBlockingDeque<>(MAX_QUEUE_SIZE);

    private final AppProperties appProperties;
    private final Clock defaultClock;
    private final AccessLog.Source source;
    private final AccessLogDao accessLogDao;
    private final AccessLogThreadCache accessLogThreadCache;
    private final Thread queueConsumerThread;

    public AccessLogServiceImpl(
            final AppProperties appProperties,
            final MeterRegistry meterRegistry,
            final Clock defaultClock,
            final AccessLog.Source source,
            final AccessLogDao accessLogDao,
            final AccessLogThreadCache accessLogThreadCache) {
        requireNonNull(meterRegistry);
        requireNonNull(appProperties);
        requireNonNull(defaultClock);
        requireNonNull(source);
        requireNonNull(accessLogDao);
        requireNonNull(accessLogThreadCache);

        this.appProperties = appProperties;
        this.defaultClock = defaultClock;
        this.source = source;
        this.accessLogDao = accessLogDao;
        this.accessLogThreadCache = accessLogThreadCache;

        // Setup and start access log queue consumer thread
        queueConsumerThread = new Thread(new AccessLogQueueConsumer());
        queueConsumerThread.setName("AccessLogQueue");
        queueConsumerThread.setDaemon(true);
        queueConsumerThread.start();

        meterRegistry.gauge(METRIC_KEY_ACCESS_LOG_SERVICE_QUEUE, -1, value -> QUEUE.size());
    }

    @Override
    public void destroy() throws InterruptedException {
        queueConsumerThread.interrupt();
        queueConsumerThread.join(10_000);
    }

    @Override
    public void create(final AccessLog.AccessLogBuilder builder, final String message, final String... arguments) {
        accessLogThreadCache.addAccessLog(
                builder
                        .withId(createId())
                        .withCreateTime(Instant.now(defaultClock))
                        .withSource(source)
                        .withMessage((isEmpty(arguments) ? message : String.format(message, (Object[]) arguments)))
                        .build()
        );
    }

    @Override
    public void processCachedAccessLogs() {
        for (final AccessLog accessLog : accessLogThreadCache.getAll()) {
            if (!QUEUE.offer(accessLog)) {
                log.warn("Access log queue is full (capacity={}), dropping log entry id='{}'",
                        MAX_QUEUE_SIZE, accessLog.getId());
            }
        }
        accessLogThreadCache.cleanup();
    }

    @Override
    public Page<AccessLogDto> getAccessLogByRequestId(final Organization organization, final String requestId) {
        return accessLogDao.listBy(
                Map.of(
                        LIST_CRITERIA_ORGANIZATION_ID, organization.getId(),
                        LIST_CRITERIA_REQUEST_ID, requestId
                ),
                PageRequest.of(0, 100)
        ).map(AccessLogDto::fromEntity);
    }

    @VisibleForTesting
    BlockingQueue<AccessLog> getQueue() {
        return QUEUE;
    }

    private class AccessLogQueueConsumer implements Runnable {
        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    final AccessLog accessLog = QUEUE.poll(appProperties.getAccessQueueProcessingPull().toMillis(), MILLISECONDS);
                    if (accessLog != null) {
                        val batch = new ArrayList<AccessLog>();
                        batch.add(accessLog);
                        QUEUE.drainTo(batch, MAX_BATCH_SIZE - 1);
                        accessLogDao.insertBatch(batch);
                    }
                }
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (final Exception e) {
                log.error("Error persisting access log batch, will retry on next cycle", e);
            }
            // Drain remaining entries on graceful shutdown
            val remaining = new ArrayList<AccessLog>();
            QUEUE.drainTo(remaining);
            if (!remaining.isEmpty()) {
                try {
                    accessLogDao.insertBatch(remaining);
                } catch (final Exception e) {
                    log.error("Failed to persist {} access log entries during shutdown", remaining.size(), e);
                }
            }
        }
    }
}
