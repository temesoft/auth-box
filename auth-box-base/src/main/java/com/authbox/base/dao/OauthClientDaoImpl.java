package com.authbox.base.dao;

import com.authbox.base.config.CacheNamesConfiguration;
import com.authbox.base.model.OauthClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@CacheConfig(cacheNames = CacheNamesConfiguration.CACHE_OAUTH_CLIENT)
public class OauthClientDaoImpl implements OauthClientDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(OauthClientDaoImpl.class);

    private final OauthClientRepository oauthClientRepository;

    public OauthClientDaoImpl(final OauthClientRepository oauthClientRepository) {
        this.oauthClientRepository = oauthClientRepository;
    }

    @Override
    public void insert(final OauthClient oauthClient) {
        LOGGER.debug("Inserting oauthClient: {}", oauthClient);
        oauthClientRepository.save(oauthClient);
    }

    @Override
    @Cacheable(key = "#id")
    public Optional<OauthClient> getById(final String id) {
        LOGGER.debug("Fetching oauthClient by id='{}'", id);
        return oauthClientRepository.findById(id);
    }

    @Override
    public Page<OauthClient> listByOrganizationId(final String organizationId, Pageable pageable) {
        LOGGER.debug("List oauthClient by organizationId='{}'", organizationId);
        final long count = oauthClientRepository.countByOrganizationId(organizationId);
        final List<OauthClient> resultList = oauthClientRepository.listByOrganizationId(organizationId, pageable);
        return new PageImpl<>(resultList, pageable, count);
    }

    @Override
    @CacheEvict(key = "#oauthClient.id")
    public void update(OauthClient oauthClient) {
        oauthClientRepository.save(oauthClient);
    }

    @Override
    @CacheEvict(key = "#id")
    public void deleteById(final String id) {
        oauthClientRepository.deleteById(id);
    }
}
