package com.authbox.base.dao;

import com.authbox.base.config.CacheNamesConfiguration;
import com.authbox.base.config.Constants;
import com.authbox.base.model.OauthToken;
import com.authbox.base.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
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
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static java.sql.Types.INTEGER;
import static java.sql.Types.VARCHAR;

@CacheConfig(cacheNames = CacheNamesConfiguration.CACHE_USER)
public class UserDaoImpl implements UserDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserDaoImpl.class);

    private final JdbcTemplate jdbcTemplate;
    private final String sqlInsert;
    private final String sqlGetById;
    private final String sqlGetByUsername;
    private final String sqlUpdate;
    private final String sqlListByOrganizationId;
    private final String sqlCountByOrganizationId;
    private final String sqlDeleteById;

    public UserDaoImpl(final JdbcTemplate jdbcTemplate, final String sqlInsert, final String sqlGetById, final String sqlGetByUsername, final String sqlUpdate, final String sqlListByOrganizationId, final String sqlCountByOrganizationId, final String sqlDeleteById) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlInsert = sqlInsert;
        this.sqlGetById = sqlGetById;
        this.sqlGetByUsername = sqlGetByUsername;
        this.sqlUpdate = sqlUpdate;
        this.sqlListByOrganizationId = sqlListByOrganizationId;
        this.sqlCountByOrganizationId = sqlCountByOrganizationId;
        this.sqlDeleteById = sqlDeleteById;
    }

    @Override
    public int insert(final User user) {
        LOGGER.debug("Inserting user: {}", user);
        return jdbcTemplate.update(sqlInsert, ps -> {
            ps.setString(1, user.id);
            ps.setTimestamp(2, Timestamp.from(user.createTime));
            ps.setString(3, user.username);
            ps.setString(4, user.password);
            ps.setString(5, user.name);
            ps.setString(6, String.join(Constants.COMMA, user.roles));
            ps.setBoolean(7, user.enabled);
            ps.setString(8, user.organizationId);
            ps.setTimestamp(9, Timestamp.from(user.lastUpdated));
        });
    }

    @Override
    @Cacheable(key = "#id", sync = true)
    public Optional<User> getById(final String id) {
        LOGGER.debug("Fetching user by id='{}'", id);
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    sqlGetById,
                    new Object[]{id},
                    new int[]{Types.VARCHAR},
                    new UserMapper()
            ));
        } catch (final EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    @Cacheable(key = "#username", sync = true)
    public Optional<User> getByUsername(final String username) {
        LOGGER.debug("Fetching user by username='{}'", username);
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    sqlGetByUsername,
                    new Object[]{username},
                    new int[]{Types.VARCHAR},
                    new UserMapper()
            ));
        } catch (final EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Page<User> listByOrganizationId(final String organizationId, final Pageable pageable) {
        final int count = Optional.ofNullable(jdbcTemplate.queryForObject(
                sqlCountByOrganizationId,
                new Object[]{organizationId},
                new int[]{VARCHAR},
                Integer.class
        )).orElse(0);
        final List<User> resultList = jdbcTemplate.query(
                sqlListByOrganizationId,
                new Object[]{organizationId, pageable.getPageSize(), pageable.getPageNumber() * pageable.getPageSize()},
                new int[]{VARCHAR, INTEGER, INTEGER},
                new UserMapper()
        );
        return new PageImpl<>(resultList, pageable, count);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(key = "#user.id"),
            @CacheEvict(key = "#user.username")
    })
    public int delete(final User user) {
        LOGGER.debug("Removing token by id='{}'", user.id);
        return jdbcTemplate.update(sqlDeleteById, new Object[]{user.id}, new int[]{VARCHAR});
    }

    @Override
    @Caching(evict = {
            @CacheEvict(key = "#id"),
            @CacheEvict(key = "#username")
    })
    public int update(final String userId, final String username, final String name, final String password, final boolean enabled, final Instant lastUpdated) {
        LOGGER.debug("Fetching user id='{}', username='{}'", userId, username);
        return jdbcTemplate.update(sqlUpdate, ps -> {
            ps.setString(1, username);
            ps.setString(2, name);
            ps.setString(3, password);
            ps.setBoolean(4, enabled);
            ps.setTimestamp(5, Timestamp.from(lastUpdated));
            ps.setString(6, userId);
        });
    }

    private static class UserMapper implements RowMapper<User> {

        @Override
        public User mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            return new User(
                    rs.getString("id"),
                    rs.getTimestamp("create_time").toInstant(),
                    rs.getString("username"),
                    rs.getString("password"),
                    rs.getString("name"),
                    Constants.CSV_SPLITTER.splitToList(rs.getString("roles_csv")),
                    rs.getBoolean("enabled"),
                    rs.getString("organization_id"),
                    rs.getTimestamp("last_updated").toInstant()
            );
        }
    }
}
