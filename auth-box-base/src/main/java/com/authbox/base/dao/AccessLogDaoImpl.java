package com.authbox.base.dao;

import com.authbox.base.model.AccessLog;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.sql.Types.INTEGER;
import static java.sql.Types.VARCHAR;

public class AccessLogDaoImpl implements AccessLogDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccessLogDaoImpl.class);
    private static final String WHERE_CLAUSE = "WHERE ";
    private static final String AND_OPERAND = " AND ";

    public static final String LIST_CRITERIA_TOKEN_ID = "tokenId";
    public static final String LIST_CRITERIA_CLIENT_ID = "clientId";
    public static final String LIST_CRITERIA_ORGANIZATION_ID = "organizationId";
    public static final String LIST_CRITERIA_REQUEST_ID = "requestId";

    private final JdbcTemplate jdbcTemplate;
    private final String sqlInsert;
    private final String sqlGetById;
    private final String sqlListBy;
    private final String sqlCountBy;

    public AccessLogDaoImpl(final JdbcTemplate jdbcTemplate, final String sqlInsert, final String sqlGetById, final String sqlListBy, final String sqlCountBy) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlInsert = sqlInsert;
        this.sqlGetById = sqlGetById;
        this.sqlListBy = sqlListBy;
        this.sqlCountBy = sqlCountBy;
    }

    @Override
    public Optional<AccessLog> getById(final String id) {
        LOGGER.debug("Fetching AccessLog by id='{}'", id);
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    sqlGetById,
                    new Object[]{id},
                    new int[]{VARCHAR},
                    new AccessLogMapper()
            ));
        } catch (final EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public int insert(final AccessLog accessLog) {
        LOGGER.debug("Inserting AccessLog: {}", accessLog);
        return jdbcTemplate.update(sqlInsert, ps -> {
            ps.setString(1, accessLog.id);
            ps.setTimestamp(2, Timestamp.from(accessLog.createTime));
            ps.setString(3, accessLog.organizationId);
            ps.setString(4, accessLog.oauthTokenId);
            ps.setString(5, accessLog.clientId);
            ps.setString(6, accessLog.requestId);
            ps.setString(7, accessLog.source.name());
            ps.setLong(8, accessLog.duration.toMillis());
            ps.setString(9, accessLog.message);
            ps.setString(10, accessLog.error);
            ps.setInt(11, accessLog.statusCode);
            ps.setString(12, accessLog.ip);
            ps.setString(13, accessLog.userAgent);
        });
    }

    @Override
    public Page<AccessLog> listBy(final Map<String, String> criteria, final Pageable pageable) {
        LOGGER.debug("List AccessLog by criteria='{}'", criteria);
        final StringBuilder whereQuery = new StringBuilder();
        final List<Integer> sqlTypes = Lists.newArrayList();
        final List<Object> values = Lists.newArrayList();
        if (!criteria.isEmpty()) {
            whereQuery.append(WHERE_CLAUSE);
            if (criteria.containsKey(LIST_CRITERIA_TOKEN_ID)) {
                whereQuery.append("oauth_token_id = ?");
                values.add(criteria.get(LIST_CRITERIA_TOKEN_ID));
                sqlTypes.add(VARCHAR);
            }
            if (criteria.containsKey(LIST_CRITERIA_CLIENT_ID)) {
                addAndOptionally(whereQuery);
                whereQuery.append("client_id = ?");
                values.add(criteria.get(LIST_CRITERIA_CLIENT_ID));
                sqlTypes.add(VARCHAR);
            }
            if (criteria.containsKey(LIST_CRITERIA_ORGANIZATION_ID)) {
                addAndOptionally(whereQuery);
                whereQuery.append("(organization_id = ? OR organization_id IS null)");
                values.add(criteria.get(LIST_CRITERIA_ORGANIZATION_ID));
                sqlTypes.add(VARCHAR);
            }
            if (criteria.containsKey(LIST_CRITERIA_REQUEST_ID)) {
                addAndOptionally(whereQuery);
                whereQuery.append("request_id = ?");
                values.add(criteria.get(LIST_CRITERIA_REQUEST_ID));
                sqlTypes.add(VARCHAR);
            }
        }

        values.add(pageable.getPageSize());
        sqlTypes.add(INTEGER);
        values.add(pageable.getPageNumber() * pageable.getPageSize());
        sqlTypes.add(INTEGER);

        final int count = Optional.ofNullable(jdbcTemplate.queryForObject(
                String.format(sqlCountBy, whereQuery),
                values.toArray(),
                sqlTypes.stream().mapToInt(x -> x).toArray(),
                Integer.class
        )).orElse(0);

        final List<AccessLog> resultList = jdbcTemplate.query(
                String.format(sqlListBy, whereQuery),
                values.toArray(),
                sqlTypes.stream().mapToInt(x -> x).toArray(),
                new AccessLogMapper()
        );
        return new PageImpl<>(resultList, pageable, count);
    }

    private void addAndOptionally(final StringBuilder whereQuery) {
        if (whereQuery.length() > WHERE_CLAUSE.length()) {
            whereQuery.append(AND_OPERAND);
        }
    }

    private static class AccessLogMapper implements RowMapper<AccessLog> {

        @Override
        public AccessLog mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            return AccessLog.AccessLogBuilder.accessLogBuilder()
                    .withClientId(rs.getString("client_id"))
                    .withDuration(Duration.ofMillis(rs.getLong("duration_ms")))
                    .withError(rs.getString("error"))
                    .withOauthTokenId(rs.getString("oauth_token_id"))
                    .withOrganizationId(rs.getString("organization_id"))
                    .withRequestId(rs.getString("request_id"))
                    .withStatusCode(rs.getInt("status_code"))
                    .withIp(rs.getString("ip"))
                    .withUserAgent(rs.getString("user_agent"))
                    .build(
                            rs.getString("id"),
                            rs.getTimestamp("create_time").toInstant(),
                            AccessLog.Source.valueOf(rs.getString("source")),
                            rs.getString("message")
                    );
        }
    }
}
