package com.authbox.base.dao;

import com.authbox.base.config.CacheNamesConfiguration;
import com.authbox.base.model.OauthClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

@CacheConfig(cacheNames = CacheNamesConfiguration.CACHE_OAUTH_CLIENT)
@AllArgsConstructor
@Slf4j
public class OauthClientDaoImpl implements OauthClientDao {


    private final OauthClientRepository oauthClientRepository;

    @Override
    public void insert(final OauthClient oauthClient) {
        log.debug("Inserting: {}", oauthClient);
        oauthClientRepository.save(oauthClient);
    }

    @Override
    @Cacheable(key = "#id")
    public Optional<OauthClient> getById(final String id) {
        log.debug("Fetching by id='{}'", id);
        return oauthClientRepository.findById(id);
    }

    @Override
    public Page<OauthClient> listByOrganizationId(final String organizationId, Pageable pageable) {
        log.debug("List by organizationId='{}'", organizationId);
        return oauthClientRepository.findAllByOrganizationId(organizationId, pageable);
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
