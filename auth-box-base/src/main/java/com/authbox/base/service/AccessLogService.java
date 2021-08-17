package com.authbox.base.service;

import com.authbox.base.model.AccessLog;

public interface AccessLogService {

    void create(AccessLog.AccessLogBuilder builder, String message, String... arguments);

    void processCachedAccessLogs();

}
