package com.authbox.base.dao;

import com.authbox.base.SpringBootBaseTest;
import com.authbox.base.config.DaoConfiguration;
import com.authbox.base.model.OauthUser;
import jakarta.transaction.Transactional;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

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
@Transactional
class OauthUserDaoTest extends SpringBootBaseTest {

    @Autowired
    private OauthUserDao oauthUserDao;
    @Autowired
    private OauthUserRepository oauthUserRepository;

    @AfterEach
    public void teardown() {
        oauthUserRepository.deleteAll();
    }

    @Test
    public void testOauthUserDao() {
        val oauthUser = OauthUser.builder()
                .withId(createId())
                .withCreateTime(Instant.now())
                .withUsername("username")
                .withOrganizationId("orgId")
                .build();
        oauthUserDao.insert(oauthUser);
        assertThat(oauthUserDao.getById(oauthUser.getId())).isPresent();
        val page = Pageable.ofSize(10);
        assertThat(oauthUserDao.listByOrganizationId("orgId", page).getTotalElements()).isEqualTo(1);
        assertThat(oauthUserDao.getByUsernameAndOrganizationId("username", "orgId")).isPresent();
        oauthUserDao.update(
                oauthUser.getId(),
                "username2",
                "password",
                false,
                "",
                false,
                Instant.now()
        );
        val updatedOauthUser = oauthUserDao.getById(oauthUser.getId());
        assertThat(updatedOauthUser).isPresent();
        assertThat(updatedOauthUser.get().isEnabled()).isFalse();
        oauthUserDao.deleteById(oauthUser.getId());
        assertThat(oauthUserDao.getById(oauthUser.getId())).isEmpty();
    }
}