package com.authbox.web.config;

import com.authbox.web.job.AccessLogCleanupJob;
import org.quartzplus.internal.ExecutionLogCleanupJob;
import org.quartzplus.service.JobsCollection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class JobsConfiguration {

    @Bean
    JobsCollection jobsCollection() {
        return () -> List.of(
                ExecutionLogCleanupJob.class,
                AccessLogCleanupJob.class
        );
    }
}
