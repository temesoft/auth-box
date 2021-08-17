package com.authbox.base.dao;

import com.authbox.base.config.CacheNamesConfiguration;
import com.authbox.base.model.OauthClient;
import com.authbox.base.model.OauthScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;
import java.util.Optional;

import static java.lang.String.join;
import static java.sql.Types.INTEGER;
import static java.sql.Types.VARCHAR;
import static java.util.Collections.nCopies;

@CacheConfig(cacheNames = CacheNamesConfiguration.CACHE_OAUTH_SCOPE)
public class OauthScopeDaoImpl implements OauthScopeDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(OauthScopeDaoImpl.class);

    private final JdbcTemplate jdbcTemplate;
    private final String sqlInsert;
    private final String sqlGetById;
    private final String sqlListByOrganizationId;
    private final String sqlCountByOrganizationId;
    private final String sqlListByIds;
    private final String sqlListByClientId;
    private final String sqlCountByOrganizationIdAndScope;
    private final String sqlDeleteById;
    private final String sqlUpdateById;

    public OauthScopeDaoImpl(final JdbcTemplate jdbcTemplate, final String sqlInsert, final String sqlGetById, final String sqlListByOrganizationId, final String sqlCountByOrganizationId, final String sqlListByIds, final String sqlListByClientId, final String sqlCountByOrganizationIdAndScope, final String sqlDeleteById, final String sqlUpdateById) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlInsert = sqlInsert;
        this.sqlGetById = sqlGetById;
        this.sqlListByOrganizationId = sqlListByOrganizationId;
        this.sqlCountByOrganizationId = sqlCountByOrganizationId;
        this.sqlListByIds = sqlListByIds;
        this.sqlListByClientId = sqlListByClientId;
        this.sqlCountByOrganizationIdAndScope = sqlCountByOrganizationIdAndScope;
        this.sqlDeleteById = sqlDeleteById;
        this.sqlUpdateById = sqlUpdateById;
    }

    @Override
    public int insert(final OauthScope oauthScope) {
        LOGGER.debug("Inserting oauthScope: {}", oauthScope);
        return jdbcTemplate.update(sqlInsert, ps -> {
            ps.setString(1, oauthScope.id);
            ps.setTimestamp(2, Timestamp.from(oauthScope.createTime));
            ps.setString(3, oauthScope.description);
            ps.setString(4, oauthScope.scope);
            ps.setString(5, oauthScope.organizationId);
        });
    }

    @Override
    @Cacheable(key = "#id", sync = true)
    public Optional<OauthScope> getById(final String id) {
        LOGGER.debug("Fetching oauthScope by id='{}'", id);
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    sqlGetById,
                    new Object[]{id},
                    new int[]{Types.VARCHAR},
                    new OauthScopeMapper()
            ));
        } catch (final EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<OauthScope> listByIds(final List<String> ids) {
        LOGGER.debug("List oauthScope by ids={}", ids);
        return jdbcTemplate.query(
                String.format(sqlListByIds, join(",", nCopies(ids.size(), "?"))),
                ids.toArray(),
                nCopies(ids.size(), VARCHAR).stream().mapToInt(x -> x).toArray(),
                new OauthScopeMapper()
        );
    }

    @Override
    public Page<OauthScope> listByOrganizationId(final String organizationId, final Pageable pageable) {
        LOGGER.debug("List oauthScope by organizationId={}", organizationId);
        final int count = Optional.ofNullable(jdbcTemplate.queryForObject(
                sqlCountByOrganizationId,
                new Object[]{organizationId},
                new int[]{VARCHAR},
                Integer.class
        )).orElse(0);
        final List<OauthScope> resultList = jdbcTemplate.query(
                sqlListByOrganizationId,
                new Object[]{organizationId, pageable.getPageSize(), pageable.getPageNumber() * pageable.getPageSize()},
                new int[]{VARCHAR, INTEGER, INTEGER},
                new OauthScopeMapper()
        );
        return new PageImpl<>(resultList, pageable, count);
    }

    @Override
    public List<OauthScope> listByClientId(final String clientId) {
        LOGGER.debug("List oauthScope by clientId={}", clientId);
        return jdbcTemplate.query(
                sqlListByClientId,
                new Object[]{clientId},
                new int[]{Types.VARCHAR},
                new OauthScopeMapper()
        );
    }

    @Override
    public boolean existsByOrganizationIdAndScope(final String organizationId, final String scope) {
        LOGGER.debug("Exists oauthScope by exists by organizationId='{}' and scope={}", organizationId, scope);
        final Integer count = jdbcTemplate.queryForObject(
                sqlCountByOrganizationIdAndScope,
                new Object[]{organizationId, scope},
                new int[]{Types.VARCHAR, Types.VARCHAR},
                Integer.class);
        return count != null && count > 0;
    }

    @Override
    @CacheEvict(key = "#id")
    public int deleteById(final String id) {
        LOGGER.debug("Delete oauthScope by id={}", id);
        return jdbcTemplate.update(sqlDeleteById, new Object[]{id}, new int[]{VARCHAR});
    }

    @Override
    @CacheEvict(key = "#id")
    public int updateById(final String id, final String scope, final String description) {
        LOGGER.debug("Update oauthScope by id={}", id);
        return jdbcTemplate.update(sqlUpdateById, new Object[]{scope, description, id}, new int[]{VARCHAR, VARCHAR, VARCHAR});
    }

    private static class OauthScopeMapper implements RowMapper<OauthScope> {

        @Override
        public OauthScope mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            return new OauthScope(
                    rs.getString("id"),
                    rs.getTimestamp("create_time").toInstant(),
                    rs.getString("description"),
                    rs.getString("scope"),
                    rs.getString("organization_id")
            );
        }
    }
}
