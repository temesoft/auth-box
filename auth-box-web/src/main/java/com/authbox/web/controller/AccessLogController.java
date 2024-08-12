package com.authbox.web.controller;

import com.authbox.base.dao.AccessLogDao;
import com.authbox.base.model.AccessLog;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static com.authbox.base.dao.AccessLogDaoImpl.LIST_CRITERIA_ORGANIZATION_ID;
import static com.authbox.base.dao.AccessLogDaoImpl.LIST_CRITERIA_REQUEST_ID;
import static com.authbox.web.config.Constants.API_PREFIX;
import static org.apache.commons.lang3.StringUtils.isBlank;

@RestController
@RequestMapping(API_PREFIX + "/access-log")
@Slf4j
public class AccessLogController extends BaseController {

    private final AccessLogDao accessLogDao;
    private final LoadingCache<String, JsonNode> cache;

    public AccessLogController(final AccessLogDao accessLogDao,
                               final RestTemplate restTemplate,
                               final ObjectMapper objectMapper,
                               @Value("${ipstack.url}") final String ipStackUrl,
                               @Value("${ipstack.enabled}") final boolean ipStackEnabled) {
        this.accessLogDao = accessLogDao;
        cache = CacheBuilder.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofDays(10))
                .build(new CacheLoader<>() {
                           @Override
                           public JsonNode load(final String ip) {
                               if (!ipStackEnabled || isBlank(ipStackUrl)) {
                                   return objectMapper.createObjectNode();
                               }
                               log.debug("Making request for IP details for: {}", ip);
                               return restTemplate.getForObject(ipStackUrl.replaceAll("\\{ip}", ip), JsonNode.class);
                           }
                       }
                );
    }

    @GetMapping("/{requestId}")
    public Page<AccessLog> getAccessLogByRequestId(@PathVariable("requestId") final String requestId) {
        val organization = getOrganization();
        return accessLogDao.listBy(
                Map.of(
                        LIST_CRITERIA_ORGANIZATION_ID, organization.getId(),
                        LIST_CRITERIA_REQUEST_ID, requestId
                ),
                PageRequest.of(0, 1000)
        );
    }

    @GetMapping("/ip/{ip}")
    public JsonNode getIpDetails(@PathVariable("ip") final String ip) throws ExecutionException {
        return cache.get(ip.trim());
    }
}