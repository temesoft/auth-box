package com.authbox.base.dao;

import com.authbox.base.config.CacheNamesConfiguration;
import com.authbox.base.model.OauthToken;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

@CacheConfig(cacheNames = CacheNamesConfiguration.CACHE_OAUTH_TOKEN)
@AllArgsConstructor
@Slf4j
public class OauthTokenDaoImpl implements OauthTokenDao {

    private final OauthTokenRepository oauthTokenRepository;

    @Override
    public void insert(final OauthToken oauthToken) {
        log.debug("Inserting: {}", oauthToken);
        oauthTokenRepository.save(oauthToken);
    }

    @Override
    @Cacheable(key = "#id", sync = true)
    public Optional<OauthToken> getById(final String id) {
        log.debug("Fetching by id='{}'", id);
        return oauthTokenRepository.findById(id);
    }

    @Override
    public Page<OauthToken> listByClientId(final String clientId, final Pageable pageable) {
        log.debug("List by clientId='{}'", clientId);
        return oauthTokenRepository.listByClientId(clientId, pageable);
    }

    @Override
    public Page<OauthToken> listByUserId(final String userId, final Pageable pageable) {
        log.debug("List by userId='{}'", userId);
        return oauthTokenRepository.listByOauthUserId(userId, pageable);
    }

    @Override
    public Page<OauthToken> listByOrganizationId(final String organizationId, final Pageable pageable) {
        log.debug("List by organizationId='{}'", organizationId);
        return oauthTokenRepository.listByOrganizationId(organizationId, pageable);
    }

    @Override
    @Cacheable(key = "#hash", sync = true)
    public Optional<OauthToken> getByHash(final String hash) {
        log.debug("Fetching by hash='{}'", hash);
        return oauthTokenRepository.findByHash(hash);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(key = "#id"),
            @CacheEvict(key = "#hash")
    })
    public void deleteById(final String id, final String hash) {
        log.debug("Removing by id='{}'", id);
        oauthTokenRepository.deleteById(id);
    }

    @Override
    public void updateLinkedTokenId(final String id, final String linkedTokenId) {
        log.debug("Updating linked id for token id='{}'", id);
        oauthTokenRepository.updateLinkedTokenId(id, linkedTokenId);
    }
}
