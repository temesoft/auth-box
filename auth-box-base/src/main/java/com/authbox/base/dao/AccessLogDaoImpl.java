package com.authbox.base.dao;

import com.authbox.base.model.AccessLog;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static java.sql.Types.INTEGER;
import static java.sql.Types.VARCHAR;

@AllArgsConstructor
public class AccessLogDaoImpl implements AccessLogDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccessLogDaoImpl.class);
    private static final String SQL_LIST_BY = "SELECT id, create_time, organization_id, oauth_token_id, client_id, request_id, source, duration_ms, message, error, status_code, ip, user_agent " +
                                              "FROM access_log %s " +
                                              "ORDER BY create_time ASC, duration_ms ASC " +
                                              "LIMIT ? " +
                                              "OFFSET ?";
    private static final String SQL_COUNT_BY = "SELECT count(id) " +
                                               "FROM access_log %s " +
                                               "ORDER BY create_time ASC, duration_ms ASC " +
                                               "LIMIT ? " +
                                               "OFFSET ?";
    private static final String WHERE_CLAUSE = "WHERE ";
    private static final String AND_OPERAND = " AND ";
    private static final String LIST_CRITERIA_TOKEN_ID = "tokenId";
    private static final String LIST_CRITERIA_CLIENT_ID = "clientId";
    public static final String LIST_CRITERIA_ORGANIZATION_ID = "organizationId";
    public static final String LIST_CRITERIA_REQUEST_ID = "requestId";

    private final JdbcTemplate jdbcTemplate;
    private final AccessLogRepository accessLogRepository;

    @Override
    public Optional<AccessLog> getById(final String id) {
        LOGGER.debug("Fetching by id='{}'", id);
        return accessLogRepository.findById(id);
    }

    @Override
    public void insert(final AccessLog accessLog) {
        LOGGER.debug("Inserting: {}", accessLog);
        accessLogRepository.save(accessLog);
    }

    @Override
    public Page<AccessLog> listBy(final Map<String, String> criteria, final Pageable pageable) {
        LOGGER.debug("List by criteria='{}'", criteria);
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
}
