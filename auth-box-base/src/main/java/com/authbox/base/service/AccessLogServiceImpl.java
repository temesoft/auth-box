package com.authbox.base.service;

import com.authbox.base.config.AppProperties;
import com.authbox.base.dao.AccessLogDao;
import com.authbox.base.model.AccessLog;
import com.authbox.base.model.Organization;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.Uninterruptibles;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import static com.authbox.base.config.Constants.METRIC_KEY_ACCESS_LOG_SERVICE_QUEUE;
import static com.authbox.base.dao.AccessLogDaoImpl.LIST_CRITERIA_ORGANIZATION_ID;
import static com.authbox.base.dao.AccessLogDaoImpl.LIST_CRITERIA_REQUEST_ID;
import static com.authbox.base.util.IdUtils.createId;
import static java.lang.Thread.MIN_PRIORITY;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;

@Slf4j
public class AccessLogServiceImpl implements AccessLogService, DisposableBean {

    private static final BlockingQueue<AccessLog> QUEUE = new LinkedBlockingDeque<>();

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
        queueConsumerThread.setPriority(MIN_PRIORITY);
        queueConsumerThread.setName("AccessLogQueue");
        queueConsumerThread.setDaemon(true);
        queueConsumerThread.start();

        meterRegistry.gauge(METRIC_KEY_ACCESS_LOG_SERVICE_QUEUE, -1, value -> QUEUE.size());
    }

    @Override
    public void destroy() {
        queueConsumerThread.interrupt();
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
        QUEUE.addAll(accessLogThreadCache.getAll());
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
                while (true) {
                    final Optional<AccessLog> accessLog = Optional.ofNullable(
                            QUEUE.poll(appProperties.getAccessQueueProcessingPull().toMillis(), MILLISECONDS)
                    );
                    accessLog.ifPresent(accessLogDao::insert);
                    if (accessLog.isEmpty() && appProperties.getAccessQueueProcessingIdle() != Duration.ZERO) {
                        Uninterruptibles.sleepUninterruptibly(appProperties.getAccessQueueProcessingIdle());
                    }
                }
            } catch (final Exception e) {
                log.info("Stopping '{}' thread, received exception: {}", this.getClass().getSimpleName(), e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
    }
}
