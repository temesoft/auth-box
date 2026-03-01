package com.authbox.base.dao;

import com.authbox.base.SpringBootBaseTest;
import com.authbox.base.config.DaoConfiguration;
import com.authbox.base.model.OauthToken;
import com.authbox.base.util.HashUtils;
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
class OauthTokenDaoTest extends SpringBootBaseTest {

    @Autowired
    private OauthTokenDao oauthTokenDao;
    @Autowired
    private OauthTokenRepository oauthTokenRepository;
    @Autowired
    private EntityManager entityManager;

    @AfterEach
    public void teardown() {
        oauthTokenRepository.deleteAll();
    }

    @Test
    public void testOauthTokenDao() {
        val hash = HashUtils.sha256("test");
        val oauthToken = OauthToken.builder()
                .withId(createId())
                .withCreateTime(Instant.now())
                .withScopes(List.of("some/scope"))
                .withHash(hash)
                .withClientId("clientId")
                .withOauthUserId("userId")
                .withOrganizationId("orgId")
                .build();
        oauthTokenDao.insert(oauthToken);

        assertThat(oauthTokenDao.getById(oauthToken.getId())).isPresent();
        assertThat(oauthTokenDao.getByHash(hash)).isPresent();
        val page = Pageable.ofSize(10);
        assertThat(oauthTokenDao.listByClientId("clientId", page).getTotalElements()).isEqualTo(1);
        assertThat(oauthTokenDao.listByUserId("userId", page).getTotalElements()).isEqualTo(1);
        assertThat(oauthTokenDao.listByOrganizationId("orgId", page).getTotalElements()).isEqualTo(1);
        oauthTokenDao.updateLinkedTokenId(oauthToken.getId(), "linkedTokenId");
        // Force synchronization to the DB and clear the cache
        entityManager.flush();
        entityManager.clear();
        assertThat(oauthToken.getLinkedTokenId()).isNull();
        val updatedOauthToken = oauthTokenDao.getById(oauthToken.getId());
        assertThat(updatedOauthToken).isPresent();
        assertThat(updatedOauthToken.get().getLinkedTokenId()).isEqualTo("linkedTokenId");
        oauthTokenDao.deleteById(oauthToken.getId(), hash);
        // Force synchronization to the DB and clear the cache
        entityManager.flush();
        entityManager.clear();
        assertThat(oauthTokenDao.getById(oauthToken.getId())).isEmpty();
    }
}