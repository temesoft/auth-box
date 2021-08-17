package com.authbox.base.dao;

import com.authbox.base.config.CacheNamesConfiguration;
import com.authbox.base.model.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.Optional;

@CacheConfig(cacheNames = CacheNamesConfiguration.CACHE_OAUTH_ORGANIZATION)
public class OrganizationDaoImpl implements OrganizationDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrganizationDaoImpl.class);

    private final JdbcTemplate jdbcTemplate;
    private final String sqlInsert;
    private final String sqlGetById;
    private final String sqlGetByDomainPrefix;
    private final String sqlUpdate;

    public OrganizationDaoImpl(final JdbcTemplate jdbcTemplate, final String sqlInsert, final String sqlGetById, final String sqlGetByDomainPrefix, final String sqlUpdate) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlInsert = sqlInsert;
        this.sqlGetById = sqlGetById;
        this.sqlGetByDomainPrefix = sqlGetByDomainPrefix;
        this.sqlUpdate = sqlUpdate;
    }

    @Override
    public int insert(final Organization organization) {
        LOGGER.debug("Inserting: {}", organization);
        return jdbcTemplate.update(sqlInsert, ps -> {
            ps.setString(1, organization.id);
            ps.setTimestamp(2, Timestamp.from(organization.createTime));
            ps.setString(3, organization.name);
            ps.setString(4, organization.domainPrefix);
            ps.setString(5, organization.address);
            ps.setBoolean(6, organization.enabled);
            ps.setTimestamp(7, Timestamp.from(organization.lastUpdated));
        });
    }

    @Override
    @Cacheable(key = "#id", sync = true)
    public Optional<Organization> getById(final String id) {
        LOGGER.debug("Fetching organization by organization_id='{}'", id);
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    sqlGetById,
                    new Object[]{id},
                    new int[]{Types.VARCHAR},
                    new OrganizationMapper()
            ));
        } catch (final EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    @Cacheable(key = "#domainPrefix", sync = true)
    public Optional<Organization> getByDomainPrefix(final String domainPrefix) {
        LOGGER.debug("Fetching organization by domain_prefix='{}'", domainPrefix);
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    sqlGetByDomainPrefix,
                    new Object[]{domainPrefix},
                    new int[]{Types.VARCHAR},
                    new OrganizationMapper()
            ));
        } catch (final EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    @Caching(evict = {
            @CacheEvict(key = "#id"),
            @CacheEvict(key = "#domainPrefix")
    })
    public int update(final String id, final String name, final String domainPrefix, final String address, final boolean enabled, final Instant lastUpdated) {
        LOGGER.debug("Updating organization by organization_id='{}'", id);
        return jdbcTemplate.update(sqlUpdate, ps -> {
            ps.setString(1, name);
            ps.setString(2, domainPrefix);
            ps.setString(3, address);
            ps.setBoolean(4, enabled);
            ps.setTimestamp(5, Timestamp.from(lastUpdated));
            ps.setString(6, id); // ID last in UPDATE statement
        });
    }

    private static class OrganizationMapper implements RowMapper<Organization> {

        @Override
        public Organization mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            return new Organization(
                    rs.getString("id"),
                    rs.getTimestamp("create_time").toInstant(),
                    rs.getString("name"),
                    rs.getString("domain_prefix"),
                    rs.getString("address"),
                    rs.getBoolean("enabled"),
                    rs.getTimestamp("last_updated").toInstant()
            );
        }
    }
}
