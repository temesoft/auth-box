package com.authbox.base.dao;

import com.authbox.base.model.OauthClientScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;
import java.util.Optional;

import static java.lang.String.join;
import static java.sql.Types.VARCHAR;
import static java.util.Collections.nCopies;

public class OauthClientScopeDaoImpl implements OauthClientScopeDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(OauthClientScopeDaoImpl.class);

    private final JdbcTemplate jdbcTemplate;
    private final String sqlInsert;
    private final String sqlGetById;
    private final String sqlListByClientId;
    private final String sqlDeleteById;
    private final String sqlDeleteByClientIdAndScopeId;
    private final String sqlDeleteByScopeId;
    private final String sqlCountClientsByScopeIds;

    public OauthClientScopeDaoImpl(final JdbcTemplate jdbcTemplate,
                                   final String sqlInsert,
                                   final String sqlGetById,
                                   final String sqlListByClientId,
                                   final String sqlDeleteById,
                                   final String sqlDeleteByClientIdAndScopeId,
                                   final String sqlDeleteByScopeId,
                                   final String sqlCountClientsByScopeIds) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlInsert = sqlInsert;
        this.sqlGetById = sqlGetById;
        this.sqlListByClientId = sqlListByClientId;
        this.sqlDeleteById = sqlDeleteById;
        this.sqlDeleteByClientIdAndScopeId = sqlDeleteByClientIdAndScopeId;
        this.sqlDeleteByScopeId = sqlDeleteByScopeId;
        this.sqlCountClientsByScopeIds = sqlCountClientsByScopeIds;
    }

    @Override
    public int insert(final OauthClientScope oauthClientScope) {
        LOGGER.debug("Inserting oauthClientScope: {}", oauthClientScope);
        return jdbcTemplate.update(sqlInsert, ps -> {
            ps.setString(1, oauthClientScope.id);
            ps.setTimestamp(2, Timestamp.from(oauthClientScope.createTime));
            ps.setString(3, oauthClientScope.clientId);
            ps.setString(4, oauthClientScope.scopeId);
        });
    }

    @Override
    public Optional<OauthClientScope> getById(final String id) {
        LOGGER.debug("Fetching oauthClientScope by id='{}'", id);
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    sqlGetById,
                    new Object[]{id},
                    new int[]{Types.VARCHAR},
                    new OauthClientScopeMapper()
            ));
        } catch (final EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<OauthClientScope> listByClientId(final String clientId) {
        LOGGER.debug("List oauthScope by clientId='{}'", clientId);
        return jdbcTemplate.query(
                sqlListByClientId,
                new Object[]{clientId},
                new int[]{Types.VARCHAR},
                new OauthClientScopeMapper()
        );
    }

    @Override
    public int countByScopeIds(final List<String> scopeIds) {
        LOGGER.debug("Count oauthScope by scopeIds={}", scopeIds);
        final Integer count = jdbcTemplate.queryForObject(
                String.format(sqlCountClientsByScopeIds, join(",", nCopies(scopeIds.size(), "?"))),
                scopeIds.toArray(),
                nCopies(scopeIds.size(), VARCHAR).stream().mapToInt(x -> x).toArray(),
                Integer.class);
        return count == null ? 0 : count;
    }

    @Override
    public int deleteById(final String id) {
        LOGGER.debug("Delete oauthScope by id='{}'", id);
        return jdbcTemplate.update(sqlDeleteById, new Object[]{id}, new int[]{VARCHAR});
    }

    @Override
    public int deleteByClientIdAndScopeId(final String clientId, final String scopeId) {
        LOGGER.debug("Delete oauthScope by clientId='{}' and scopeId='{}'", clientId, scopeId);
        return jdbcTemplate.update(sqlDeleteByClientIdAndScopeId, new Object[]{clientId, scopeId}, new int[]{VARCHAR, VARCHAR});
    }

    @Override
    public int deleteByScopeId(final String scopeId) {
        LOGGER.debug("Delete oauthScope by scopeId='{}'", scopeId);
        return jdbcTemplate.update(sqlDeleteByScopeId, new Object[]{scopeId}, new int[]{VARCHAR});
    }

    private static class OauthClientScopeMapper implements RowMapper<OauthClientScope> {

        @Override
        public OauthClientScope mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            return new OauthClientScope(
                    rs.getString("id"),
                    rs.getTimestamp("create_time").toInstant(),
                    rs.getString("client_id"),
                    rs.getString("scope_id")
            );
        }
    }
}
