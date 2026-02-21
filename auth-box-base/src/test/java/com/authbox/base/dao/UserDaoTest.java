package com.authbox.base.dao;

import com.authbox.base.SpringBootBaseTest;
import com.authbox.base.config.DaoConfiguration;
import com.authbox.base.model.User;
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
class UserDaoTest extends SpringBootBaseTest {

    @Autowired
    private UserDao userDao;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EntityManager entityManager;

    @AfterEach
    public void teardown() {
        userRepository.deleteAll();
    }

    @Test
    public void testUserDao() {
        val user = User.builder()
                .withId(createId())
                .withCreateTime(Instant.now())
                .withUsername("username")
                .withRoles(List.of("ROLE_USER", "ROLE_ADMIN"))
                .withEnabled(true)
                .withOrganizationId("orgId")
                .build();
        userDao.insert(user);
        assertThat(userDao.getById(user.getId())).isPresent();
        userDao.update(
                user.getId(),
                "username",
                "Mr. Tester",
                "password",
                false,
                Instant.now()
        );
        // Force synchronization to the DB and clear the cache
        entityManager.flush();
        entityManager.clear();
        val updatedUser = userDao.getById(user.getId());
        assertThat(updatedUser).isPresent();
        assertThat(updatedUser.get().isEnabled()).isFalse();
        assertThat(userDao.getByUsername("username")).isPresent();
        val page = Pageable.ofSize(10);
        assertThat(userDao.listByOrganizationId("orgId", page).getTotalElements()).isEqualTo(1);
        userDao.delete(user);
        assertThat(userDao.getById(user.getId())).isEmpty();
    }
}