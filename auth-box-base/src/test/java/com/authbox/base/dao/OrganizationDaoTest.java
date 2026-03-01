package com.authbox.base.dao;

import com.authbox.base.SpringBootBaseTest;
import com.authbox.base.config.DaoConfiguration;
import com.authbox.base.model.Organization;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

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
class OrganizationDaoTest extends SpringBootBaseTest {

    @Autowired
    private OrganizationDao organizationDao;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private EntityManager entityManager;

    @AfterEach
    public void teardown() {
        organizationRepository.deleteAll();
    }

    @Test
    public void testOrganizationDao() {
        val organization = Organization.builder()
                .withId(createId())
                .withCreateTime(Instant.now())
                .withName("localhost")
                .withDomainPrefix("local")
                .withEnabled(true)
                .build();
        organizationDao.insert(organization);
        assertThat(organizationDao.getById(organization.getId())).isPresent();
        assertThat(organizationDao.getByDomainPrefix("local")).isPresent();
        organizationDao.update(
                organization.getId(),
                "name",
                "domain",
                "address",
                false,
                "logout-url",
                Instant.now()
        );
        // Force synchronization to the DB and clear the cache
        entityManager.flush();
        entityManager.clear();
        val updatedOrganization = organizationDao.getById(organization.getId());
        assertThat(updatedOrganization).isPresent();
        assertThat(updatedOrganization.get().isEnabled()).isFalse();

        organizationDao.deleteById(organization.getId());
        assertThat(organizationDao.getById(organization.getId())).isEmpty();
    }
}