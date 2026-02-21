package com.authbox.base.dao;

import com.authbox.base.SpringBootBaseTest;
import com.authbox.base.config.DaoConfiguration;
import com.authbox.base.model.AccessLog;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static com.authbox.base.dao.AccessLogDaoImpl.LIST_CRITERIA_CLIENT_ID;
import static com.authbox.base.dao.AccessLogDaoImpl.LIST_CRITERIA_ORGANIZATION_ID;
import static com.authbox.base.dao.AccessLogDaoImpl.LIST_CRITERIA_REQUEST_ID;
import static com.authbox.base.dao.AccessLogDaoImpl.LIST_CRITERIA_TOKEN_ID;
import static com.authbox.base.util.IdUtils.createId;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = {
                DataSourceAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class,
                DaoConfiguration.class
        },
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class AccessLogDaoTest extends SpringBootBaseTest {

    @Autowired
    private AccessLogDao accessLogDao;
    @Autowired
    private AccessLogRepository accessLogRepository;

    @AfterEach
    public void teardown() {
        accessLogRepository.deleteAll();
    }

    @Test
    public void testAccessLogDao() {
        val id = createId();
        accessLogDao.insert(
                AccessLog.builder()
                        .withClientId("clientId")
                        .withCreateTime(Instant.now())
                        .withDuration(Duration.ofSeconds(2))
                        .withId(id)
                        .withIp("1.2.3.4")
                        .withStatusCode(201)
                        .withOauthTokenId("token")
                        .withSource(AccessLog.Source.Oauth2Server)
                        .withOrganizationId("orgId")
                        .withRequestId("requestId")
                        .build()
        );

        assertThat(accessLogDao.getById(id)).isPresent();

        val page = Pageable.ofSize(10);
        assertThat(accessLogDao.listBy(Map.of(), page).getTotalElements())
                .isEqualTo(1);
        assertThat(accessLogDao.listBy(Map.of(LIST_CRITERIA_TOKEN_ID, "token"), page).getTotalElements())
                .isEqualTo(1);
        assertThat(accessLogDao.listBy(Map.of(LIST_CRITERIA_CLIENT_ID, "clientId"), page).getTotalElements())
                .isEqualTo(1);
        assertThat(accessLogDao.listBy(Map.of(LIST_CRITERIA_ORGANIZATION_ID, "orgId"), page).getTotalElements())
                .isEqualTo(1);
        assertThat(accessLogDao.listBy(Map.of(LIST_CRITERIA_REQUEST_ID, "requestId"), page).getTotalElements())
                .isEqualTo(1);
    }
}