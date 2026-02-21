package com.authbox.base.dao;

import com.authbox.base.SpringBootBaseTest;
import com.authbox.base.config.DaoConfiguration;
import com.authbox.base.model.GrantType;
import com.authbox.base.model.OauthClient;
import com.authbox.base.model.TokenFormat;
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
class OauthClientDaoTest extends SpringBootBaseTest {

    @Autowired
    private OauthClientDao oauthClientDao;
    @Autowired
    private OauthClientRepository oauthClientRepository;

    @AfterEach
    public void teardown() {
        oauthClientRepository.deleteAll();
    }

    @Test
    public void testOauthClientDao() {
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

        assertThat(oauthClientDao.getById(client.getId())).isPresent();
        val page = Pageable.ofSize(10);
        assertThat(oauthClientDao.listByOrganizationId("organizationId", page).getTotalElements())
                .isEqualTo(1);
        assertThat(client.isEnabled()).isTrue();
        client.setEnabled(false);
        oauthClientDao.update(client);
        val updatedClient = oauthClientDao.getById(client.getId());
        assertThat(updatedClient).isPresent();
        assertThat(updatedClient.get().isEnabled()).isFalse();

        oauthClientDao.deleteById(client.getId());
        assertThat(oauthClientDao.getById(client.getId())).isEmpty();
    }
}