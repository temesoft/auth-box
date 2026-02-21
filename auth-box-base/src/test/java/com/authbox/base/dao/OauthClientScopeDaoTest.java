package com.authbox.base.dao;

import com.authbox.base.SpringBootBaseTest;
import com.authbox.base.config.DaoConfiguration;
import com.authbox.base.model.GrantType;
import com.authbox.base.model.OauthClient;
import com.authbox.base.model.OauthClientScope;
import com.authbox.base.model.OauthScope;
import com.authbox.base.model.TokenFormat;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
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
class OauthClientScopeDaoTest extends SpringBootBaseTest {

    @Autowired
    private OauthClientScopeDao oauthClientScopeDao;
    @Autowired
    private OauthClientScopeRepository oauthClientScopeRepository;
    @Autowired
    private OauthClientDao oauthClientDao;
    @Autowired
    private OauthClientRepository oauthClientRepository;
    @Autowired
    private OauthScopeDao oauthScopeDao;
    @Autowired
    private OauthScopeRepository oauthScopeRepository;
    @Autowired
    private EntityManager entityManager;

    @AfterEach
    public void teardown() {
        oauthClientScopeRepository.deleteAll();
        oauthClientRepository.deleteAll();
        oauthScopeRepository.deleteAll();
    }

    @Test
    public void testOauthClientScopeDao() {
        val client = new OauthClient(
                createId(),
                Instant.now(),
                "description",
                "secret",
                List.of(GrantType.client_credentials, GrantType.refresh_token),
                "organizationId",
                true,
                List.of("https://some.domain/redirect1", "https://other.domain/redirect2"),
                Duration.ofHours(1),
                Duration.ofHours(2),
                TokenFormat.STANDARD,
                "priv-key",
                "pub-key",
                Instant.now(),
                List.of(),
                List.of()
        );
        oauthClientDao.insert(client);

        val oauthScope = OauthScope.builder()
                .withId(createId())
                .withDescription("description")
                .withOrganizationId("orgId")
                .withScope("some/scope")
                .withCreateTime(Instant.now())
                .build();
        oauthScopeDao.insert(oauthScope);

        val id = createId();
        val oauthClientScope = OauthClientScope.builder()
                .withId(id)
                .withScopeId(oauthScope.getId())
                .withClientId(client.getId())
                .withCreateTime(Instant.now())
                .build();
        oauthClientScopeDao.insert(oauthClientScope);
        assertThat(oauthClientScopeDao.getById(id)).isPresent();
        assertThat(oauthClientScopeDao.listByClientId(client.getId())).hasSize(1);
        assertThat(oauthClientScopeDao.countByScopeIds(List.of(oauthScope.getId()))).isEqualTo(1);

        oauthClientScopeDao.deleteByClientIdAndScopeId(client.getId(), oauthScope.getId());
        // Force synchronization to the DB and clear the cache
        entityManager.flush();
        entityManager.clear();
        assertThat(oauthClientScopeDao.getById(id)).isEmpty();

        oauthClientScopeDao.insert(oauthClientScope);
        assertThat(oauthClientScopeDao.getById(id)).isPresent();
        oauthClientScopeDao.deleteByScopeId(oauthScope.getId());
        // Force synchronization to the DB and clear the cache
        entityManager.flush();
        entityManager.clear();
        assertThat(oauthClientScopeDao.getById(id)).isEmpty();

        oauthClientScopeDao.insert(oauthClientScope);
        assertThat(oauthClientScopeDao.getById(id)).isPresent();
        oauthClientScopeDao.deleteById(id);
        // Force synchronization to the DB and clear the cache
        entityManager.flush();
        entityManager.clear();
        assertThat(oauthClientScopeDao.getById(id)).isEmpty();
    }
}