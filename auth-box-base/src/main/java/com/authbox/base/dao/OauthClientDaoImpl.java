package com.authbox.base.dao;

import com.authbox.base.config.CacheNamesConfiguration;
import com.authbox.base.config.Constants;
import com.authbox.base.model.GrantType;
import com.authbox.base.model.OauthClient;
import com.authbox.base.model.TokenFormat;
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
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static java.sql.Types.INTEGER;
import static java.sql.Types.VARCHAR;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toUnmodifiableList;

@CacheConfig(cacheNames = CacheNamesConfiguration.CACHE_OAUTH_CLIENT)
public class OauthClientDaoImpl implements OauthClientDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(OauthClientDaoImpl.class);

    private final JdbcTemplate jdbcTemplate;
    private final String sqlInsert;
    private final String sqlGetById;
    private final String sqlListByOrganizationId;
    private final String sqlCountByOrganizationId;
    private final String sqlUpdateById;
    private final String sqlDeleteById;

    public OauthClientDaoImpl(final JdbcTemplate jdbcTemplate, final String sqlInsert, final String sqlGetById, final String sqlListByOrganizationId, final String sqlCountByOrganizationId, final String sqlUpdateById, final String sqlDeleteById) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlInsert = sqlInsert;
        this.sqlGetById = sqlGetById;
        this.sqlListByOrganizationId = sqlListByOrganizationId;
        this.sqlCountByOrganizationId = sqlCountByOrganizationId;
        this.sqlUpdateById = sqlUpdateById;
        this.sqlDeleteById = sqlDeleteById;
    }

    @Override
    public int insert(final OauthClient oauthClient) {
        LOGGER.debug("Inserting oauthClient: {}", oauthClient);
        return jdbcTemplate.update(sqlInsert, ps -> {
            ps.setString(1, oauthClient.id);
            ps.setTimestamp(2, Timestamp.from(oauthClient.createTime));
            ps.setString(3, oauthClient.description);
            ps.setString(4, oauthClient.secret);
            ps.setString(5, oauthClient.grantTypes.stream().map(Enum::name).collect(joining(Constants.COMMA)));
            ps.setString(6, oauthClient.organizationId);
            ps.setBoolean(7, oauthClient.enabled);
            ps.setString(8, String.join(Constants.COMMA, oauthClient.redirectUrls));
            ps.setLong(9, oauthClient.expiration.toSeconds());
            ps.setLong(10, oauthClient.refreshExpiration.toSeconds());
            ps.setString(11, oauthClient.tokenFormat.name());
            ps.setString(12, oauthClient.privateKey);
            ps.setString(13, oauthClient.publicKey);
            ps.setTimestamp(14, Timestamp.from(oauthClient.lastUpdated));
        });
    }

    @Override
    @Cacheable(key = "#id")
    public Optional<OauthClient> getById(final String id) {
        LOGGER.debug("Fetching oauthClient by id='{}'", id);
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    sqlGetById,
                    new Object[]{id},
                    new int[]{VARCHAR},
                    new OauthClientMapper()
            ));
        } catch (final EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Page<OauthClient> listByOrganizationId(final String organizationId, Pageable pageable) {
        LOGGER.debug("List oauthClient by organizationId='{}'", organizationId);
        final int count = Optional.ofNullable(jdbcTemplate.queryForObject(
                sqlCountByOrganizationId,
                new Object[]{organizationId},
                new int[]{VARCHAR},
                Integer.class
        )).orElse(0);
        final List<OauthClient> resultList = jdbcTemplate.query(
                sqlListByOrganizationId,
                new Object[]{organizationId, pageable.getPageSize(), pageable.getPageNumber() * pageable.getPageSize()},
                new int[]{VARCHAR, INTEGER, INTEGER},
                new OauthClientMapper()
        );

        return new PageImpl<>(resultList, pageable, count);
    }

    @Override
    @CacheEvict(key = "#id")
    public int updateById(final String id,
                          final String description,
                          final String grantTypesCsv,
                          final boolean enabled,
                          final String redirectUrlsCsv,
                          final Duration expiration,
                          final Duration refreshExpiration,
                          final TokenFormat tokenFormat,
                          final String privateKey,
                          final String publicKey,
                          final Instant lastUpdated) {
        LOGGER.debug("Update oauthClient by id='{}'", id);
        return jdbcTemplate.update(sqlUpdateById, ps -> {
            ps.setString(1, description);
            ps.setString(2, grantTypesCsv);
            ps.setBoolean(3, enabled);
            ps.setString(4, redirectUrlsCsv);
            ps.setLong(5, expiration.toSeconds());
            ps.setLong(6, refreshExpiration.toSeconds());
            ps.setString(7, tokenFormat.name());
            ps.setString(8, privateKey);
            ps.setString(9, publicKey);
            ps.setTimestamp(10, Timestamp.from(lastUpdated));
            ps.setString(11, id);
        });
    }

    @Override
    @CacheEvict(key = "#id")
    public int deleteById(final String id) {
        LOGGER.debug("Delete oauthClient by id={}", id);
        return jdbcTemplate.update(sqlDeleteById, ps -> {
            ps.setString(1, id);
        });
    }

    private static class OauthClientMapper implements RowMapper<OauthClient> {

        @Override
        public OauthClient mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            return new OauthClient(
                    rs.getString("id"),
                    rs.getTimestamp("create_time").toInstant(),
                    rs.getString("description"),
                    rs.getString("secret"),
                    Constants.CSV_SPLITTER.splitToList(rs.getString("grant_types_csv")).stream().map(GrantType::valueOf).collect(toUnmodifiableList()),
                    rs.getString("organization_id"),
                    rs.getBoolean("enabled"),
                    Constants.CSV_SPLITTER.splitToList(rs.getString("redirect_urls_csv")),
                    Duration.ofSeconds(rs.getLong("expiration_seconds")),
                    Duration.ofSeconds(rs.getLong("refresh_expiration_seconds")),
                    TokenFormat.valueOf(rs.getString("token_type")),
                    rs.getString("private_key"),
                    rs.getString("public_key"),
                    rs.getTimestamp("last_updated").toInstant()
            );
        }
    }
}
