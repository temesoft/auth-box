package com.authbox.base.config;

import com.authbox.base.dao.AccessLogDao;
import com.authbox.base.dao.AccessLogDaoImpl;
import com.authbox.base.dao.AccessLogRepository;
import com.authbox.base.dao.OauthClientDao;
import com.authbox.base.dao.OauthClientDaoImpl;
import com.authbox.base.dao.OauthClientRepository;
import com.authbox.base.dao.OauthClientScopeDao;
import com.authbox.base.dao.OauthClientScopeDaoImpl;
import com.authbox.base.dao.OauthClientScopeRepository;
import com.authbox.base.dao.OauthScopeDao;
import com.authbox.base.dao.OauthScopeDaoImpl;
import com.authbox.base.dao.OauthScopeRepository;
import com.authbox.base.dao.OauthTokenDao;
import com.authbox.base.dao.OauthTokenDaoImpl;
import com.authbox.base.dao.OauthTokenRepository;
import com.authbox.base.dao.OauthUserDao;
import com.authbox.base.dao.OauthUserDaoImpl;
import com.authbox.base.dao.OauthUserRepository;
import com.authbox.base.dao.OrganizationDao;
import com.authbox.base.dao.OrganizationDaoImpl;
import com.authbox.base.dao.OrganizationRepository;
import com.authbox.base.dao.UserDao;
import com.authbox.base.dao.UserDaoImpl;
import com.authbox.base.dao.UserRepository;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
@EntityScan(basePackages = "com.authbox.base.*", basePackageClasses = {Jsr310JpaConverters.class})
@EnableJpaRepositories(basePackages = "com.authbox.base.dao")
public class DaoConfiguration {

    @Bean
    JdbcTemplate jdbcTemplate(final DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    UserDao userDao(final UserRepository userRepository) {
        return new UserDaoImpl(userRepository);
    }

    @Bean
    OrganizationDao organizationDao(final OrganizationRepository organizationRepository) {
        return new OrganizationDaoImpl(organizationRepository);
    }

    @Bean
    OauthClientDao oauthClientDao(final OauthClientRepository oauthClientRepository) {
        return new OauthClientDaoImpl(oauthClientRepository);
    }

    @Bean
    OauthTokenDao oauthTokenDao(final OauthTokenRepository oauthTokenRepository) {
        return new OauthTokenDaoImpl(oauthTokenRepository);
    }

    @Bean
    OauthUserDao oauthUserDao(final OauthUserRepository oauthUserRepository) {
        return new OauthUserDaoImpl(oauthUserRepository);
    }

    @Bean
    OauthScopeDao oauthScopeDao(final OauthScopeRepository oauthScopeRepository) {
        return new OauthScopeDaoImpl(oauthScopeRepository);
    }

    @Bean
    OauthClientScopeDao oauthClientScopeDao(final OauthClientScopeRepository oauthClientScopeRepository) {
        return new OauthClientScopeDaoImpl(oauthClientScopeRepository);
    }

    @Bean
    AccessLogDao accessLogDao(final JdbcTemplate jdbcTemplate, AccessLogRepository accessLogRepository) {
        return new AccessLogDaoImpl(jdbcTemplate, accessLogRepository);
    }
}

