package com.authbox.base.dao;

import com.authbox.base.model.AccessLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AccessLogDao {

    void insert(AccessLog accessLog);

    void insertBatch(List<AccessLog> accessLogs);

    Optional<AccessLog> getById(String id);

    Page<AccessLog> listBy(Map<String, String> criteria, Pageable pageable);

    int deleteAllBeforeDate(Instant date);

}
