package com.authbox.base.service;

import com.authbox.base.model.AccessLog;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AccessLogThreadCache {

    private static ThreadLocal<List<AccessLog>> threadLocal = ThreadLocal.withInitial(ArrayList::new);

    public void cleanup() {
        threadLocal.remove();
    }

    public void addAccessLog(final AccessLog accessLog) {
        threadLocal.get().add(accessLog);
    }

    public List<AccessLog> getAll() {
        return threadLocal.get();
    }
}
