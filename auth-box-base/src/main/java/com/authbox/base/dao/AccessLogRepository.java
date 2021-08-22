package com.authbox.base.dao;

import com.authbox.base.model.AccessLog;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccessLogRepository extends CrudRepository<AccessLog, String> {
}
