package com.authbox.base.config;

import com.authbox.base.dao.AccessLogDao;
import com.authbox.base.dao.AccessLogDaoImpl;
import com.authbox.base.dao.OauthClientDao;
import com.authbox.base.dao.OauthClientDaoImpl;
import com.authbox.base.dao.OauthClientScopeDao;
import com.authbox.base.dao.OauthClientScopeDaoImpl;
import com.authbox.base.dao.OauthScopeDao;
import com.authbox.base.dao.OauthScopeDaoImpl;
import com.authbox.base.dao.OauthTokenDao;
import com.authbox.base.dao.OauthTokenDaoImpl;
import com.authbox.base.dao.OauthUserDao;
import com.authbox.base.dao.OauthUserDaoImpl;
import com.authbox.base.dao.OrganizationDao;
import com.authbox.base.dao.OrganizationDaoImpl;
import com.authbox.base.dao.UserDao;
import com.authbox.base.dao.UserDaoImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
@PropertySource("classpath:sql.statements.properties")
public class DaoConfiguration {

    @Bean
    JdbcTemplate jdbcTemplate(final DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    UserDao userDao(final JdbcTemplate jdbcTemplate,
                    @Value("${user.sql.insert}") final String sqlInsert,
                    @Value("${user.sql.getById}") final String sqlGetById,
                    @Value("${user.sql.getByUsername}") final String sqlGetByUsername,
                    @Value("${user.sql.update}") final String sqlUpdate,
                    @Value("${user.sql.listByOrganizationId}") final String sqlListByOrganizationId,
                    @Value("${user.sql.countByOrganizationId}") final String sqlCountByOrganizationId,
                    @Value("${user.sql.deleteById}") final String sqlDeleteById) {
        return new UserDaoImpl(jdbcTemplate, sqlInsert, sqlGetById, sqlGetByUsername, sqlUpdate, sqlListByOrganizationId, sqlCountByOrganizationId, sqlDeleteById);
    }

    @Bean
    OrganizationDao organizationDao(final JdbcTemplate jdbcTemplate,
                                    @Value("${organization.sql.insert}") final String sqlInsert,
                                    @Value("${organization.sql.getById}") final String sqlGetById,
                                    @Value("${organization.sql.getByDomainPrefix}") final String sqlGetByDomainPrefix,
                                    @Value("${organization.sql.update}") final String sqlUpdate) {
        return new OrganizationDaoImpl(jdbcTemplate, sqlInsert, sqlGetById, sqlGetByDomainPrefix, sqlUpdate);
    }

    @Bean
    OauthClientDao oauthClientDao(final JdbcTemplate jdbcTemplate,
                                  @Value("${oauth_client.sql.insert}") final String sqlInsert,
                                  @Value("${oauth_client.sql.getById}") final String sqlGetById,
                                  @Value("${oauth_client.sql.listByOrganizationId}") final String sqlListByOrganizationId,
                                  @Value("${oauth_client.sql.countByOrganizationId}") final String sqlCountByOrganizationId,
                                  @Value("${oauth_client.sql.updateById}") final String sqlUpdateById,
                                  @Value("${oauth_client.sql.deleteById}") final String sqlDeleteById) {
        return new OauthClientDaoImpl(jdbcTemplate, sqlInsert, sqlGetById, sqlListByOrganizationId, sqlCountByOrganizationId, sqlUpdateById, sqlDeleteById);
    }

    @Bean
    OauthTokenDao oauthTokenDao(final JdbcTemplate jdbcTemplate,
                                @Value("${oauth_token.sql.insert}") final String sqlInsert,
                                @Value("${oauth_token.sql.getById}") final String sqlGetById,
                                @Value("${oauth_token.sql.getByHash}") final String sqlGetByHash,
                                @Value("${oauth_token.sql.deleteById}") final String sqlDeleteById,
                                @Value("${oauth_token.sql.listByClientId}") final String sqlListByClientId,
                                @Value("${oauth_token.sql.countByClientId}") final String sqlCountByClientId,
                                @Value("${oauth_token.sql.listByUserId}") final String sqlListByUserId,
                                @Value("${oauth_token.sql.countByUserId}") final String sqlCountByUserId,
                                @Value("${oauth_token.sql.listByOrganizationId}") final String sqlListByOrganizationId,
                                @Value("${oauth_token.sql.countByOrganizationId}") final String sqlCountByOrganizationId,
                                @Value("${oauth_token.sql.updateLinkedTokenId}") final String sqlUpdateLinkedTokenId) {
        return new OauthTokenDaoImpl(jdbcTemplate, sqlInsert, sqlGetById, sqlGetByHash, sqlDeleteById, sqlListByClientId,
                sqlCountByClientId, sqlListByUserId, sqlCountByUserId, sqlListByOrganizationId,
                sqlCountByOrganizationId, sqlUpdateLinkedTokenId);
    }

    @Bean
    OauthUserDao oauthUserDao(final JdbcTemplate jdbcTemplate,
                              @Value("${oauth_user.sql.insert}") final String sqlInsert,
                              @Value("${oauth_user.sql.getById}") final String sqlGetById,
                              @Value("${oauth_user.sql.getByUsernameAndOrganizationId}") final String sqlGetByUsernameAndOrganizationId,
                              @Value("${oauth_user.sql.listByOrganizationId}") final String sqlListByOrganizationId,
                              @Value("${oauth_user.sql.countByOrganizationId}") final String sqlCountByOrganizationId,
                              @Value("${oauth_user.sql.deleteById}") final String sqlDeleteById,
                              @Value("${oauth_user.sql.updateById}") final String sqlUpdateById) {
        return new OauthUserDaoImpl(jdbcTemplate, sqlInsert, sqlGetById, sqlGetByUsernameAndOrganizationId,
                sqlListByOrganizationId, sqlCountByOrganizationId, sqlDeleteById, sqlUpdateById);
    }

    @Bean
    OauthScopeDao oauthScopeDao(final JdbcTemplate jdbcTemplate,
                                @Value("${oauth_scope.sql.insert}") final String sqlInsert,
                                @Value("${oauth_scope.sql.getById}") final String sqlGetById,
                                @Value("${oauth_scope.sql.listByOrganizationId}") final String sqlListByOrganizationId,
                                @Value("${oauth_scope.sql.countByOrganizationId}") final String sqlCountByOrganizationId,
                                @Value("${oauth_scope.sql.listByIds}") final String sqlListByIds,
                                @Value("${oauth_scope.sql.listByClientId}") final String sqlListByClientId,
                                @Value("${oauth_scope.sql.countByOrganizationIdAndScope}") final String sqlCountByOrganizationIdAndScope,
                                @Value("${oauth_scope.sql.deleteById}") final String sqlDeleteById,
                                @Value("${oauth_scope.sql.updateById}") final String sqlUpdateById) {
        return new OauthScopeDaoImpl(jdbcTemplate, sqlInsert, sqlGetById, sqlListByOrganizationId, sqlCountByOrganizationId,
                sqlListByIds, sqlListByClientId, sqlCountByOrganizationIdAndScope, sqlDeleteById, sqlUpdateById);
    }

    @Bean
    OauthClientScopeDao oauthClientScopeDao(final JdbcTemplate jdbcTemplate,
                                            @Value("${oauth_client_scope.sql.insert}") final String sqlInsert,
                                            @Value("${oauth_client_scope.sql.getById}") final String sqlGetById,
                                            @Value("${oauth_client_scope.sql.listByClientId}") final String sqlListByClientId,
                                            @Value("${oauth_client_scope.sql.deleteById}") final String sqlDeleteById,
                                            @Value("${oauth_client_scope.sql.deleteByClientIdAndScopeId}") final String sqlDeleteByClientIdAndScopeId,
                                            @Value("${oauth_client_scope.sql.deleteByScopeId}") final String sqlDeleteByScopeId,
                                            @Value("${oauth_client_scope.sql.countClientsByScopeIds}") final String sqlCountClientsByScopeIds) {
        return new OauthClientScopeDaoImpl(jdbcTemplate, sqlInsert, sqlGetById, sqlListByClientId, sqlDeleteById,
                sqlDeleteByClientIdAndScopeId, sqlDeleteByScopeId, sqlCountClientsByScopeIds);
    }

    @Bean
    AccessLogDao accessLogDao(final JdbcTemplate jdbcTemplate,
                              @Value("${access_log.sql.insert}") final String sqlInsert,
                              @Value("${access_log.sql.getById}") final String sqlGetById,
                              @Value("${access_log.sql.listBy}") final String sqlListBy,
                              @Value("${access_log.sql.countBy}") final String sqlCountBy) {
        return new AccessLogDaoImpl(jdbcTemplate, sqlInsert, sqlGetById, sqlListBy, sqlCountBy);
    }
}

