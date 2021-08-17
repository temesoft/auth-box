package com.authbox.base.dao;

import com.authbox.base.model.AccessLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.Optional;

public interface AccessLogDao {

    int insert(AccessLog accessLog);

    Optional<AccessLog> getById(String id);

    Page<AccessLog> listBy(Map<String, String> criteria, Pageable pageable);

}
