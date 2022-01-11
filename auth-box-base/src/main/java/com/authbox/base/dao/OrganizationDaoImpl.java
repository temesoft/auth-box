package com.authbox.base.dao;

import com.authbox.base.config.CacheNamesConfiguration;
import com.authbox.base.model.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

import java.time.Instant;
import java.util.Optional;

@CacheConfig(cacheNames = CacheNamesConfiguration.CACHE_OAUTH_ORGANIZATION)
public class OrganizationDaoImpl implements OrganizationDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrganizationDaoImpl.class);

    private final OrganizationRepository organizationRepository;

    public OrganizationDaoImpl(final OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    @Override
    public void insert(final Organization organization) {
        LOGGER.debug("Inserting: {}", organization);
        organizationRepository.save(organization);
    }

    @Override
    @Cacheable(key = "#id", sync = true)
    public Optional<Organization> getById(final String id) {
        LOGGER.debug("Fetching by organization_id='{}'", id);
        return organizationRepository.findById(id);
    }

    @Override
    @Cacheable(key = "#domainPrefix", sync = true)
    public Optional<Organization> getByDomainPrefix(final String domainPrefix) {
        LOGGER.debug("Fetching by domain_prefix='{}'", domainPrefix);
        return organizationRepository.findByDomainPrefix(domainPrefix);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(key = "#id"),
            @CacheEvict(key = "#domainPrefix")
    })
    public void update(final String id, final String name, final String domainPrefix, final String address, final boolean enabled, final String logoUrl, final Instant lastUpdated) {
        LOGGER.debug("Updating by organization_id='{}'", id);
        organizationRepository.update(id, name, domainPrefix, address, enabled, logoUrl, lastUpdated);
    }
}
