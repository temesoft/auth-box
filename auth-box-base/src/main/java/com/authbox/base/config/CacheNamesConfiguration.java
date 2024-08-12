package com.authbox.base.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class CacheNamesConfiguration {

    public static final String CACHE_OAUTH_CLIENT = "OauthClient";
    public static final String CACHE_OAUTH_SCOPE = "OauthScope";
    public static final String CACHE_OAUTH_TOKEN = "OauthToken";
    public static final String CACHE_OAUTH_USER = "OauthUser";
    public static final String CACHE_OAUTH_ORGANIZATION = "Organization";
    public static final String CACHE_USER = "User";

    @Bean
    @ConditionalOnMissingBean
    List<CacheName> cacheNames() {
        return List.of(CacheName.values());
    }

    public enum CacheName {
        CACHE_USER(CacheNamesConfiguration.CACHE_USER),
        CACHE_OAUTH_ORGANIZATION(CacheNamesConfiguration.CACHE_OAUTH_ORGANIZATION),
        CACHE_OAUTH_USER(CacheNamesConfiguration.CACHE_OAUTH_USER),
        CACHE_OAUTH_TOKEN(CacheNamesConfiguration.CACHE_OAUTH_TOKEN),
        CACHE_OAUTH_SCOPE(CacheNamesConfiguration.CACHE_OAUTH_SCOPE),
        CACHE_OAUTH_CLIENT(CacheNamesConfiguration.CACHE_OAUTH_CLIENT);

        public final String cacheName;

        CacheName(final String cacheName) {
            this.cacheName = cacheName;
        }
    }
}
