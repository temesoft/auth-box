package com.authbox.base.config;

import com.authbox.base.model.AccessLog;
import com.authbox.base.service.AccessLogService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import lombok.val;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.util.Pair;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Configuration
@AllArgsConstructor
public class StartupTasksConfiguration {

    private final AccessLogService accessLogService;
    private final AccessLog.Source source;

    @PostConstruct
    public void postConstructTasks() throws UnknownHostException {
        val ipAndHost = ipAndHostname();
        val message = String.format("%s startup on ip='%s', hostname='%s'", source, ipAndHost.getFirst(), ipAndHost.getSecond());
        accessLogService.create(AccessLog.builder(), message);
        accessLogService.processCachedAccessLogs();
    }

    @PreDestroy
    public void preDestroyTasks() throws UnknownHostException {
        val ipAndHost = ipAndHostname();
        val message = String.format("%s shutdown on ip='%s', hostname='%s'", source, ipAndHost.getFirst(), ipAndHost.getSecond());
        accessLogService.create(AccessLog.builder(), message);
        accessLogService.processCachedAccessLogs();
    }

    private Pair<String, String> ipAndHostname() throws UnknownHostException {
        val ip = InetAddress.getLocalHost();
        return Pair.of(ip.getHostAddress(), ip.getHostName());
    }
}
