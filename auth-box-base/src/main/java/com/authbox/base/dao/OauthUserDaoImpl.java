package com.authbox.base.dao;

import com.authbox.base.config.CacheNamesConfiguration;
import com.authbox.base.model.OauthToken;
import com.authbox.base.model.OauthUser;
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
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static java.sql.Types.INTEGER;
import static java.sql.Types.VARCHAR;

@CacheConfig(cacheNames = CacheNamesConfiguration.CACHE_OAUTH_USER)
public class OauthUserDaoImpl implements OauthUserDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(OauthUserDaoImpl.class);

    private final JdbcTemplate jdbcTemplate;
    private final String sqlInsert;
    private final String sqlGetById;
    private final String sqlGetByUsernameAndOrganizationId;
    private final String sqlListByOrganizationId;
    private final String sqlCountByOrganizationId;
    private final String sqlDeleteById;
    private final String sqlUpdateById;

    public OauthUserDaoImpl(final JdbcTemplate jdbcTemplate, final String sqlInsert, final String sqlGetById, final String sqlGetByUsernameAndOrganizationId, final String sqlListByOrganizationId, final String sqlCountByOrganizationId, final String sqlDeleteById, final String sqlUpdateById) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlInsert = sqlInsert;
        this.sqlGetById = sqlGetById;
        this.sqlGetByUsernameAndOrganizationId = sqlGetByUsernameAndOrganizationId;
        this.sqlListByOrganizationId = sqlListByOrganizationId;
        this.sqlCountByOrganizationId = sqlCountByOrganizationId;
        this.sqlDeleteById = sqlDeleteById;
        this.sqlUpdateById = sqlUpdateById;
    }

    @Override
    public int insert(final OauthUser oauthUser) {
        LOGGER.debug("Inserting oauthUser: {}", oauthUser);
        return jdbcTemplate.update(sqlInsert, ps -> {
            ps.setString(1, oauthUser.id);
            ps.setTimestamp(2, Timestamp.from(oauthUser.createTime));
            ps.setString(3, oauthUser.username);
            ps.setString(4, oauthUser.password);
            ps.setBoolean(5, oauthUser.enabled);
            ps.setString(6, oauthUser.organizationId);
            ps.setString(7, oauthUser.metadata);
            ps.setBoolean(8, oauthUser.using2Fa);
            ps.setString(9, oauthUser.secret);
            ps.setTimestamp(10, Timestamp.from(oauthUser.lastUpdated));
        });
    }

    @Override
    @Cacheable(key = "#id", sync = true)
    public Optional<OauthUser> getById(final String id) {
        LOGGER.debug("Fetching oauthUser by id='{}'", id);
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    sqlGetById,
                    new Object[]{id},
                    new int[]{VARCHAR},
                    new OauthUserMapper()
            ));
        } catch (final EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<OauthUser> getByUsernameAndOrganizationId(final String username, final String organizationId) {
        LOGGER.debug("Fetching oauthUser by username='{}' and organizationId='{}'", username, organizationId);
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    sqlGetByUsernameAndOrganizationId,
                    new Object[]{username, organizationId},
                    new int[]{VARCHAR, VARCHAR},
                    new OauthUserMapper()
            ));
        } catch (final EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Page<OauthUser> listByOrganizationId(final String organizationId, final Pageable pageable) {
        LOGGER.debug("List oauthUser(s) by organizationId='{}'", organizationId);
        final int count = Optional.ofNullable(jdbcTemplate.queryForObject(
                sqlCountByOrganizationId,
                new Object[]{organizationId},
                new int[]{VARCHAR},
                Integer.class
        )).orElse(0);
        final List<OauthUser> resultList = jdbcTemplate.query(
                sqlListByOrganizationId,
                new Object[]{organizationId, pageable.getPageSize(), pageable.getPageNumber() * pageable.getPageSize()},
                new int[]{VARCHAR, INTEGER, INTEGER},
                new OauthUserMapper()
        );
        return new PageImpl<>(resultList, pageable, count);
    }

    @Override
    @CacheEvict(key = "#id")
    public int deleteById(final String id) {
        LOGGER.debug("Fetching oauthUser by id='{}'", id);
        return jdbcTemplate.update(sqlDeleteById, ps -> {
            ps.setString(1, id);
        });
    }

    @Override
    @CacheEvict(key = "#id")
    public int update(final String id, final String username, final String password, final boolean enabled, final String metadata, final boolean using2Fa, final Instant lastUpdated) {
        LOGGER.debug("Updating oauthUser by id='{}', username='{}'", id, username);
        return jdbcTemplate.update(sqlUpdateById, ps -> {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setBoolean(3, enabled);
            ps.setString(4, metadata);
            ps.setBoolean(5, using2Fa);
            ps.setTimestamp(6, Timestamp.from(lastUpdated));
            ps.setString(7, id);
        });
    }

    private static class OauthUserMapper implements RowMapper<OauthUser> {

        @Override
        public OauthUser mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            return new OauthUser(
                    rs.getString("id"),
                    rs.getTimestamp("create_time").toInstant(),
                    rs.getString("username"),
                    rs.getString("password"),
                    rs.getBoolean("enabled"),
                    rs.getString("organization_id"),
                    rs.getString("metadata"),
                    rs.getBoolean("using_2fa"),
                    rs.getString("secret"),
                    rs.getTimestamp("last_updated").toInstant()
            );
        }
    }
}
