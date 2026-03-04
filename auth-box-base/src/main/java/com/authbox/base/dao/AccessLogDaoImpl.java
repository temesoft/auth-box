package com.authbox.base.dao;

import com.authbox.base.model.AccessLog;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.sql.Types.INTEGER;
import static java.sql.Types.VARCHAR;

@AllArgsConstructor
@Slf4j
@SuppressWarnings("InlineFormatString")
public class AccessLogDaoImpl implements AccessLogDao {

    public static final String LIST_CRITERIA_TOKEN_ID = "tokenId";
    public static final String LIST_CRITERIA_CLIENT_ID = "clientId";
    public static final String LIST_CRITERIA_ORGANIZATION_ID = "organizationId";
    public static final String LIST_CRITERIA_REQUEST_ID = "requestId";

    private static final String SQL_INSERT = "INSERT INTO access_log " +
            "(id, create_time, organization_id, oauth_token_id, client_id, request_id, source, duration_ms, message, error, status_code, ip, user_agent) " +
            "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
    private static final String SQL_LIST_BY = "SELECT id, create_time, organization_id, oauth_token_id, client_id, request_id, source, duration_ms, message, error, status_code, ip, user_agent " +
            "FROM access_log %s " +
            "ORDER BY create_time ASC, duration_ms ASC " +
            "LIMIT ? " +
            "OFFSET ?";
    private static final String SQL_COUNT_BY = "SELECT count(id) " +
            "FROM access_log %s " +
            "LIMIT ? " +
            "OFFSET ?";
    private static final String WHERE_CLAUSE = "WHERE ";
    private static final String AND_OPERAND = " AND ";

    private final JdbcTemplate jdbcTemplate;
    private final AccessLogRepository accessLogRepository;

    @Override
    public Optional<AccessLog> getById(final String id) {
        log.debug("Fetching by id='{}'", id);
        return accessLogRepository.findById(id);
    }

    @Override
    public void insert(final AccessLog accessLog) {
        log.debug("Inserting: {}", accessLog);
        jdbcTemplate.update(SQL_INSERT, ps -> setInsertParameters(ps, accessLog));
    }

    @Override
    public void insertBatch(final List<AccessLog> accessLogs) {
        log.debug("Batch inserting: {} entries", accessLogs.size());
        jdbcTemplate.batchUpdate(SQL_INSERT, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(final PreparedStatement ps, final int i) throws SQLException {
                setInsertParameters(ps, accessLogs.get(i));
            }

            @Override
            public int getBatchSize() {
                return accessLogs.size();
            }
        });
    }

    @Override
    public Page<AccessLog> listBy(final Map<String, String> criteria, final Pageable pageable) {
        log.debug("List by criteria='{}'", criteria);
        val whereQuery = new StringBuilder();
        val sqlTypes = Lists.<Integer>newArrayList();
        val values = Lists.newArrayList();
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

        val count = Optional.ofNullable(jdbcTemplate.queryForObject(
                String.format(SQL_COUNT_BY, whereQuery),
                values.toArray(),
                sqlTypes.stream().mapToInt(x -> x).toArray(),
                Integer.class
        )).orElse(0);

        val resultList = jdbcTemplate.query(
                String.format(SQL_LIST_BY, whereQuery),
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
            return AccessLog.builder()
                    .withClientId(rs.getString("client_id"))
                    .withDuration(Duration.ofMillis(rs.getLong("duration_ms")))
                    .withError(rs.getString("error"))
                    .withOauthTokenId(rs.getString("oauth_token_id"))
                    .withOrganizationId(rs.getString("organization_id"))
                    .withRequestId(rs.getString("request_id"))
                    .withStatusCode(rs.getInt("status_code"))
                    .withIp(rs.getString("ip"))
                    .withUserAgent(rs.getString("user_agent"))
                    .withId(rs.getString("id"))
                    .withCreateTime(rs.getTimestamp("create_time").toInstant())
                    .withSource(AccessLog.Source.valueOf(rs.getString("source")))
                    .withMessage(rs.getString("message"))
                    .build();
        }
    }

    private void setInsertParameters(final PreparedStatement ps, final AccessLog accessLog) throws SQLException {
        ps.setString(1, accessLog.getId());
        ps.setTimestamp(2, accessLog.getCreateTime() != null ? Timestamp.from(accessLog.getCreateTime()) : null);
        ps.setString(3, accessLog.getOrganizationId());
        ps.setString(4, accessLog.getOauthTokenId());
        ps.setString(5, accessLog.getClientId());
        ps.setString(6, accessLog.getRequestId());
        ps.setString(7, accessLog.getSource() != null ? accessLog.getSource().name() : null);
        ps.setObject(8, accessLog.getDuration() != null ? accessLog.getDuration().toMillis() : null);
        ps.setString(9, accessLog.getMessage());
        ps.setString(10, accessLog.getError());
        ps.setInt(11, accessLog.getStatusCode());
        ps.setString(12, accessLog.getIp());
        ps.setString(13, accessLog.getUserAgent());
    }
}
