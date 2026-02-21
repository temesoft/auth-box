package com.authbox.base.service;

import com.authbox.base.SpringBootBaseTest;
import com.authbox.base.config.AppProperties;
import com.authbox.base.config.DaoConfiguration;
import com.authbox.base.dao.AccessLogDao;
import com.authbox.base.dao.OrganizationDao;
import com.authbox.base.model.AccessLog;
import com.authbox.base.model.Organization;
import com.github.ksuid.Ksuid;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import static com.authbox.base.model.AccessLog.Source.Oauth2Server;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        classes = {
                DataSourceAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class,
                DaoConfiguration.class
        },
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class AccessLogServiceTest extends SpringBootBaseTest {

    private static final String ORGANIZATION_ID = Ksuid.newKsuid().toString();

    @MockitoSpyBean
    private AccessLogDao accessLogDao;
    @Autowired
    private OrganizationDao organizationDao;

    @BeforeEach
    public void setup() {
        organizationDao.insert(
                new Organization(
                        ORGANIZATION_ID,
                        Instant.now(),
                        "localhost",
                        "localhost",
                        "localhost",
                        true,
                        null,
                        Instant.now()
                )
        );
    }

    @AfterEach
    public void teardown() {
        organizationDao.deleteById(ORGANIZATION_ID);
    }

    @Test
    public void testAccessLogQueueProcessing() {
        val accessLogThreadCache = new AccessLogThreadCache();
        val service = new AccessLogServiceImpl(
                new AppProperties(),
                new SimpleMeterRegistry(),
                Clock.systemUTC(),
                Oauth2Server,
                accessLogDao,
                accessLogThreadCache
        );
        val organization = organizationDao.getById(ORGANIZATION_ID);
        assertThat(organization).isPresent();
        val requestId = Ksuid.newKsuid().toString();
        service.create(AccessLog.builder()
                        .withOrganizationId(organization.get().getId())
                        .withRequestId(requestId)
                        .withDuration(Duration.ofSeconds(2))
                , "Test message");
        service.processCachedAccessLogs();
        await().atMost(Duration.ofSeconds(2)).until(() -> service.getQueue().isEmpty());
        verify(accessLogDao, times(1)).insert(any());

        val pageOfLogs = service.getAccessLogByRequestId(organization.get(), requestId);
        assertThat(pageOfLogs).isNotNull();
        assertThat(pageOfLogs.getTotalElements()).isEqualTo(1);
    }
}