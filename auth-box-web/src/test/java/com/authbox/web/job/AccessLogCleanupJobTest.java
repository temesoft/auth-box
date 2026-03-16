package com.authbox.web.job;

import com.authbox.base.dao.AccessLogRepository;
import com.authbox.base.model.AccessLog;
import com.authbox.web.Application;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.Map;

import static com.authbox.base.util.IdUtils.createId;
import static com.authbox.web.job.AccessLogCleanupJob.DAYS_AGO_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(
        classes = {Application.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureRestTestClient
@TestPropertySource(locations = "classpath:application-test.properties")
@Import(AccessLogCleanupJob.class)
class AccessLogCleanupJobTest {

    @Autowired
    private AccessLogRepository accessLogRepository;
    @Autowired
    private AccessLogCleanupJob accessLogCleanupJob;

    @Test
    void testJobExecution() {
        accessLogRepository.save(AccessLog.builder()
                .withId(createId())
                .withCreateTime(Instant.now())
                .withSource(AccessLog.Source.WebManagementPortal)
                .withMessage("test message")
                .build());
        val jobExecutionContext = mock(JobExecutionContext.class);

        when(jobExecutionContext.getMergedJobDataMap()).thenReturn(new JobDataMap());
        accessLogCleanupJob.executeJob(jobExecutionContext);
        assertThat(accessLogRepository.findAll()).isNotEmpty();

        when(jobExecutionContext.getMergedJobDataMap()).thenReturn(new JobDataMap(Map.of(DAYS_AGO_KEY, -2)));
        accessLogCleanupJob.executeJob(jobExecutionContext);
        assertThat(accessLogRepository.findAll()).isEmpty();
    }
}