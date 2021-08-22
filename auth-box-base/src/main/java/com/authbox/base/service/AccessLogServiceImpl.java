package com.authbox.base.service;

import com.authbox.base.dao.AccessLogDao;
import com.authbox.base.model.AccessLog;
import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import static java.lang.Thread.MIN_PRIORITY;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;

public class AccessLogServiceImpl implements AccessLogService, DisposableBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccessLogServiceImpl.class);
    private static final BlockingQueue<AccessLog> QUEUE = new LinkedBlockingDeque<>();

    private final Clock defaultClock;
    private final AccessLog.Source source;
    private final AccessLogDao accessLogDao;
    private final AccessLogThreadCache accessLogThreadCache;
    private final Thread queueConsumerThread;

    public AccessLogServiceImpl(final Clock defaultClock,
                                final AccessLog.Source source,
                                final AccessLogDao accessLogDao,
                                final AccessLogThreadCache accessLogThreadCache) {
        requireNonNull(defaultClock);
        requireNonNull(source);
        requireNonNull(accessLogDao);
        requireNonNull(accessLogThreadCache);

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
    }

    @Override
    public void destroy() {
        queueConsumerThread.interrupt();
    }

    @Override
    public void create(final AccessLog.AccessLogBuilder builder, final String message, final String... arguments) {
        accessLogThreadCache.addAccessLog(
                builder
                        .withId(UUID.randomUUID().toString())
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

    @VisibleForTesting
    BlockingQueue<AccessLog> getQueue() {
        return QUEUE;
    }

    private class AccessLogQueueConsumer implements Runnable {

        public void run() {
            try {
                while (true) {
                    accessLogDao.insert(QUEUE.take());
                }
            } catch (final Exception e) {
                LOGGER.info("Stopping '{}' thread", this.getClass().getSimpleName());
                Thread.currentThread().interrupt();
            }
        }
    }
}
