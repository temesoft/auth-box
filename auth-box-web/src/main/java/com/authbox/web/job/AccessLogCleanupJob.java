package com.authbox.web.job;

import ch.qos.logback.classic.Level;
import com.authbox.base.service.AccessLogService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import lombok.val;
import org.quartz.JobExecutionContext;
import org.quartzplus.Job;
import org.quartzplus.annotation.CronTriggerSpec;
import org.quartzplus.annotation.JobSpec;
import org.quartzplus.annotation.TriggerSpec;
import org.quartzplus.annotation.TriggerState;
import org.quartzplus.configuration.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Calendar;
import java.util.SimpleTimeZone;

/**
 * Internal maintenance job responsible for purging historical access log entries from the database.
 * <p>
 * This job targets the {@code access_log} table and removes records older than a specified threshold
 * to prevent unbounded table growth. By default, it runs daily at 03:00 UTC.</p>
 *
 * <h3>Configuration</h3>
 * <ul>
 *     <li><b>Enable/Disable:</b> {@code job-access-log-cleanup-job.enabled} (Default: true)</li>
 *     <li><b>Schedule:</b> {@code job-access-log-cleanup-job.cron-expression} (Default: {@code 0 0 3 * * ?})</li>
 *     <li><b>Time Zone:</b> {@code job-access-log-cleanup-job.time-zone} (Default: UTC)</li>
 *     <li><b>Retention Period:</b> {@code com.authbox.web.job.AccessLogCleanupJob.daysAgo} (Default: 365 days)</li>
 * </ul>
 *
 * <h3>Dynamic Parameters</h3>
 * The retention period can be overridden at runtime by providing a {@code daysAgo} key in the
 * JobDataMap (e.g., via a JSON parameter {@code {"daysAgo": 90}}).
 *
 * @see Job
 * @see AccessLogService
 */
@JobSpec(jobName = "AccessLogCleanupJob",
        groupName = Constants.GROUP_NAME_INTERNAL,
        triggerName = "AccessLogCleanupJob-Trigger",
        jobDescription = "Job cleans old access log entries (db table: access_log), by default older than 365 days ago. " +
                "Optionally takes json parameter \"daysAgo\" to overwrite the original setting. Example {\"daysAgo\":7}",
        triggerState = @TriggerState(enabledExp = "${job-access-log-cleanup-job.enabled:true}"),
        trigger = @TriggerSpec(
                cronTrigger = @CronTriggerSpec(
                        cronExpressionExp = "${job-access-log-cleanup-job.cron-expression:0 0 3 * * ?}",
                        timeZoneExp = "${job-access-log-cleanup-job.time-zone:UTC}"
                )
        )
)
public class AccessLogCleanupJob extends Job {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccessLogCleanupJob.class);

    @VisibleForTesting
    static final String DAYS_AGO_KEY = "daysAgo";

    @Autowired
    private AccessLogService accessLogService;

    @Value("${com.authbox.web.job.AccessLogCleanupJob.daysAgo:365}")
    private int daysAgo;

    /**
     * Sets the log capture level to {@code DEBUG} for this specific maintenance task.
     *
     * @return {@link Level#DEBUG}
     */
    @Override
    public Level getLoggerLevel() {
        return Level.DEBUG;
    }

    /**
     * Executes the cleanup logic.
     * <p>
     * Calculates the cutoff date based on {@code daysAgo}, invokes the {@link AccessLogService}
     * to perform the deletion, and updates the execution context with the count of cleared records.</p>
     *
     * @param jobExecutionContext the Quartz execution context containing potential parameter overrides.
     * @throws RuntimeException if the database deletion fails.
     */
    @Override
    public void executeJob(final JobExecutionContext jobExecutionContext) {
        LOGGER.debug("Starting access_log cleanup job");
        val stopwatch = Stopwatch.createStarted();
        val cal = Calendar.getInstance(SimpleTimeZone.getDefault());
        if (jobExecutionContext.getMergedJobDataMap().containsKey(DAYS_AGO_KEY)) {
            daysAgo = jobExecutionContext.getMergedJobDataMap().getInt(DAYS_AGO_KEY);
        }
        LOGGER.debug("Will try to clear records older than {} days ago", daysAgo);
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo);
        try {
            val cleared = accessLogService.clearAccessLogEntriesBeforeDate(cal.toInstant());
            jobExecutionContext.getMergedJobDataMap().put("clearedRecords", cleared);
            jobExecutionContext.getMergedJobDataMap().put("since", cal.toInstant());
            if (!jobExecutionContext.getMergedJobDataMap().containsKey(DAYS_AGO_KEY)) {
                jobExecutionContext.getMergedJobDataMap().put(DAYS_AGO_KEY, daysAgo);
            }
            LOGGER.info("Finished in {}, cleared {} records", stopwatch.stop(), cleared);
        } catch (final Exception e) {
            throw new RuntimeException("Problem cleaning access_log table: " + e.getMessage(), e);
        }
    }
}
