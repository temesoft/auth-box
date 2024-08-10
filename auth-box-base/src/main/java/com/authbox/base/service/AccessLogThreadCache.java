package com.authbox.base.service;

import com.authbox.base.model.AccessLog;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AccessLogThreadCache {

    private static final ThreadLocal<List<AccessLog>> THREAD_LOCAL_CACHE = ThreadLocal.withInitial(ArrayList::new);

    public void cleanup() {
        THREAD_LOCAL_CACHE.remove();
    }

    public void addAccessLog(final AccessLog accessLog) {
        THREAD_LOCAL_CACHE.get().add(accessLog);
    }

    public List<AccessLog> getAll() {
        return THREAD_LOCAL_CACHE.get();
    }
}
