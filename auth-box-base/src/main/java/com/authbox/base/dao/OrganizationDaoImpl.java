package com.authbox.base.dao;

import com.authbox.base.config.CacheNamesConfiguration;
import com.authbox.base.model.Organization;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

import java.time.Instant;
import java.util.Optional;

@CacheConfig(cacheNames = CacheNamesConfiguration.CACHE_OAUTH_ORGANIZATION)
@AllArgsConstructor
@Slf4j
public class OrganizationDaoImpl implements OrganizationDao {

    private final OrganizationRepository organizationRepository;

    @Override
    public void insert(final Organization organization) {
        log.debug("Inserting: {}", organization);
        organizationRepository.save(organization);
    }

    @Override
    @Cacheable(key = "#id", sync = true)
    public Optional<Organization> getById(final String id) {
        log.debug("Fetching by organization_id='{}'", id);
        return organizationRepository.findById(id);
    }

    @Override
    @Cacheable(key = "#domainPrefix", sync = true)
    public Optional<Organization> getByDomainPrefix(final String domainPrefix) {
        log.debug("Fetching by domain_prefix='{}'", domainPrefix);
        return organizationRepository.findByDomainPrefix(domainPrefix);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(key = "#id"),
            @CacheEvict(key = "#domainPrefix")
    })
    public void update(final String id,
                       final String name,
                       final String domainPrefix,
                       final String address,
                       final boolean enabled,
                       final String logoUrl,
                       final Instant lastUpdated) {
        log.debug("Updating by organization_id='{}'", id);
        organizationRepository.update(id, name, domainPrefix, address, enabled, logoUrl, lastUpdated);
    }
}
