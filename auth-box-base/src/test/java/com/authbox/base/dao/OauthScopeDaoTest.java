package com.authbox.base.dao;

import com.authbox.base.SpringBootBaseTest;
import com.authbox.base.config.DaoConfiguration;
import com.authbox.base.model.OauthScope;
import jakarta.persistence.EntityManager;
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
import java.util.List;

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
class OauthScopeDaoTest extends SpringBootBaseTest {

    @Autowired
    private OauthScopeDao oauthScopeDao;
    @Autowired
    private OauthScopeRepository oauthScopeRepository;
    @Autowired
    private EntityManager entityManager;

    @AfterEach
    public void teardown() {
        oauthScopeRepository.deleteAll();
    }

    @Test
    public void testOauthScopeDao() {
        val oauthScope = OauthScope.builder()
                .withId(createId())
                .withDescription("description")
                .withOrganizationId("orgId")
                .withScope("some/scope")
                .withCreateTime(Instant.now())
                .build();
        oauthScopeDao.insert(oauthScope);

        assertThat(oauthScopeDao.getById(oauthScope.getId())).isPresent();
        assertThat(oauthScopeDao.existsByOrganizationIdAndScope("orgId", "some/scope")).isTrue();
        assertThat(oauthScopeDao.existsByOrganizationIdAndScope("orgId2", "some/scope")).isFalse();
        assertThat(oauthScopeDao.existsByOrganizationIdAndScope("orgId", "other/scope")).isFalse();
        assertThat(oauthScopeDao.listByIds(List.of(oauthScope.getId()))).hasSize(1);
        val page = Pageable.ofSize(10);
        assertThat(oauthScopeDao.listByOrganizationId("orgId", page).getTotalElements()).isEqualTo(1);
        oauthScopeDao.update(oauthScope.getId(), "some/scope", "updated description");
        // Force synchronization to the DB and clear the cache
        entityManager.flush();
        entityManager.clear();
        val updatedOauthScope = oauthScopeDao.getById(oauthScope.getId());
        assertThat(updatedOauthScope).isPresent();
        assertThat(updatedOauthScope.get().getDescription()).isEqualTo("updated description");

        oauthScopeDao.deleteById(oauthScope.getId());
        assertThat(oauthScopeDao.getById(oauthScope.getId())).isEmpty();
    }

}