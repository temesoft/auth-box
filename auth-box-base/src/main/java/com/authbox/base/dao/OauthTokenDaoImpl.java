package com.authbox.base.dao;

import com.authbox.base.config.CacheNamesConfiguration;
import com.authbox.base.config.Constants;
import com.authbox.base.model.OauthToken;
import com.authbox.base.model.TokenType;
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
import java.util.List;
import java.util.Optional;

import static java.sql.Types.INTEGER;
import static java.sql.Types.VARCHAR;

@CacheConfig(cacheNames = CacheNamesConfiguration.CACHE_OAUTH_TOKEN)
public class OauthTokenDaoImpl implements OauthTokenDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(OauthTokenDaoImpl.class);

    private final JdbcTemplate jdbcTemplate;
    private final String sqlInsert;
    private final String sqlGetById;
    private final String sqlGetByHash;
    private final String sqlDeleteById;
    private final String sqlListByClientId;
    private final String sqlCountByClientId;
    private final String sqlListByUserId;
    private final String sqlCountByUserId;
    private final String sqlListByOrganizationId;
    private final String sqlCountByOrganizationId;
    private final String sqlUpdateLinkedTokenId;

    public OauthTokenDaoImpl(final JdbcTemplate jdbcTemplate, final String sqlInsert, final String sqlGetById, final String sqlGetByHash, final String sqlDeleteById, final String sqlListByClientId, final String sqlCountByClientId, final String sqlListByUserId, final String sqlCountByUserId, final String sqlListByOrganizationId, final String sqlCountByOrganizationId, final String sqlUpdateLinkedTokenId) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlInsert = sqlInsert;
        this.sqlGetById = sqlGetById;
        this.sqlGetByHash = sqlGetByHash;
        this.sqlDeleteById = sqlDeleteById;
        this.sqlListByClientId = sqlListByClientId;
        this.sqlCountByClientId = sqlCountByClientId;
        this.sqlListByUserId = sqlListByUserId;
        this.sqlCountByUserId = sqlCountByUserId;
        this.sqlListByOrganizationId = sqlListByOrganizationId;
        this.sqlCountByOrganizationId = sqlCountByOrganizationId;
        this.sqlUpdateLinkedTokenId = sqlUpdateLinkedTokenId;
    }

    @Override
    public int insert(final OauthToken oauthToken) {
        LOGGER.debug("Inserting token: {}", oauthToken);
        return jdbcTemplate.update(sqlInsert, ps -> {
            ps.setString(1, oauthToken.id);
            ps.setTimestamp(2, Timestamp.from(oauthToken.createTime));
            ps.setString(3, oauthToken.hash);
            ps.setString(4, oauthToken.organizationId);
            ps.setString(5, oauthToken.clientId);
            ps.setTimestamp(6, Timestamp.from(oauthToken.expiration));
            ps.setString(7, String.join(Constants.COMMA, oauthToken.scopes));
            ps.setString(8, oauthToken.oauthUserId);
            ps.setString(9, oauthToken.tokenType.name());
            ps.setString(10, oauthToken.ip);
            ps.setString(11, oauthToken.userAgent);
            ps.setString(12, oauthToken.requestId);
        });
    }

    @Override
    @Cacheable(key = "#id", sync = true)
    public Optional<OauthToken> getById(final String id) {
        LOGGER.debug("Fetching token by id='{}'", id);
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    sqlGetById,
                    new Object[]{id},
                    new int[]{VARCHAR},
                    new OauthTokenMapper()
            ));
        } catch (final EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Page<OauthToken> listByClientId(final String clientId, final Pageable pageable) {
        LOGGER.debug("List token(s) by clientId='{}'", clientId);
        final int count = Optional.ofNullable(jdbcTemplate.queryForObject(
                sqlCountByClientId,
                new Object[]{clientId},
                new int[]{VARCHAR},
                Integer.class
        )).orElse(0);
        final List<OauthToken> resultList = jdbcTemplate.query(
                sqlListByClientId,
                new Object[]{clientId, pageable.getPageSize(), pageable.getPageNumber() * pageable.getPageSize()},
                new int[]{VARCHAR, INTEGER, INTEGER},
                new OauthTokenMapper()
        );

        return new PageImpl<>(resultList, pageable, count);
    }

    @Override
    public Page<OauthToken> listByUserId(final String userId, final Pageable pageable) {
        LOGGER.debug("List token(s) by userId='{}'", userId);
        final int count = Optional.ofNullable(jdbcTemplate.queryForObject(
                sqlCountByUserId,
                new Object[]{userId},
                new int[]{VARCHAR},
                Integer.class
        )).orElse(0);
        final List<OauthToken> resultList = jdbcTemplate.query(
                sqlListByUserId,
                new Object[]{userId, pageable.getPageSize(), pageable.getPageNumber() * pageable.getPageSize()},
                new int[]{VARCHAR, INTEGER, INTEGER},
                new OauthTokenMapper()
        );

        return new PageImpl<>(resultList, pageable, count);
    }

    @Override
    public Page<OauthToken> listByOrganizationId(final String organizationId, final Pageable pageable) {
        LOGGER.debug("List token(s) by organizationId='{}'", organizationId);
        final int count = Optional.ofNullable(jdbcTemplate.queryForObject(
                sqlCountByOrganizationId,
                new Object[]{organizationId},
                new int[]{VARCHAR},
                Integer.class
        )).orElse(0);
        final List<OauthToken> resultList = jdbcTemplate.query(
                sqlListByOrganizationId,
                new Object[]{organizationId, pageable.getPageSize(), pageable.getPageNumber() * pageable.getPageSize()},
                new int[]{VARCHAR, INTEGER, INTEGER},
                new OauthTokenMapper()
        );
        return new PageImpl<>(resultList, pageable, count);
    }

    @Override
    @Cacheable(key = "#hash", sync = true)
    public Optional<OauthToken> getByHash(final String hash) {
        LOGGER.debug("Fetching token by hash='{}'", hash);
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    sqlGetByHash,
                    new Object[]{hash},
                    new int[]{VARCHAR},
                    new OauthTokenMapper()
            ));
        } catch (final EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    @Caching(evict = {
            @CacheEvict(key = "#id"),
            @CacheEvict(key = "#hash")
    })
    public int deleteById(final String id, final String hash) {
        LOGGER.debug("Removing token by id='{}'", id);
        return jdbcTemplate.update(sqlDeleteById, new Object[]{id}, new int[]{VARCHAR});
    }

    @Override
    public int updateLinkedTokenId(final String id, final String linkedTokenId) {
        return jdbcTemplate.update(sqlUpdateLinkedTokenId, new Object[]{linkedTokenId, id}, new int[]{VARCHAR, VARCHAR});
    }

    private static class OauthTokenMapper implements RowMapper<OauthToken> {

        @Override
        public OauthToken mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            return new OauthToken(
                    rs.getString("id"),
                    rs.getTimestamp("create_time").toInstant(),
                    rs.getString("hash"),
                    rs.getString("organization_id"),
                    rs.getString("client_id"),
                    rs.getTimestamp("expiration").toInstant(),
                    Constants.CSV_SPLITTER.splitToList(rs.getString("scopes_csv")),
                    rs.getString("oauth_user_id"),
                    TokenType.valueOf(rs.getString("token_type")),
                    rs.getString("ip"),
                    rs.getString("user_agent"),
                    rs.getString("request_id")
            ).withLinkedTokenId(rs.getString("linked_token_id"));
        }
    }
}
