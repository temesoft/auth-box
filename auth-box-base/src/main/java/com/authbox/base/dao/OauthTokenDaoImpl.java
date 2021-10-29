package com.authbox.base.dao;

import com.authbox.base.config.CacheNamesConfiguration;
import com.authbox.base.model.OauthToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@CacheConfig(cacheNames = CacheNamesConfiguration.CACHE_OAUTH_TOKEN)
public class OauthTokenDaoImpl implements OauthTokenDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(OauthTokenDaoImpl.class);

    private final OauthTokenRepository oauthTokenRepository;

    public OauthTokenDaoImpl(final OauthTokenRepository oauthTokenRepository) {
        this.oauthTokenRepository = oauthTokenRepository;
    }

    @Override
    public void insert(final OauthToken oauthToken) {
        LOGGER.debug("Inserting: {}", oauthToken);
        oauthTokenRepository.save(oauthToken);
    }

    @Override
    @Cacheable(key = "#id", sync = true)
    public Optional<OauthToken> getById(final String id) {
        LOGGER.debug("Fetching by id='{}'", id);
        return oauthTokenRepository.findById(id);
    }

    @Override
    public Page<OauthToken> listByClientId(final String clientId, final Pageable pageable) {
        LOGGER.debug("List by clientId='{}'", clientId);
        final long count = oauthTokenRepository.countByClientId(clientId);
        final List<OauthToken> resultList = oauthTokenRepository.listByClientId(clientId, pageable);
        return new PageImpl<>(resultList, pageable, count);
    }

    @Override
    public Page<OauthToken> listByUserId(final String userId, final Pageable pageable) {
        LOGGER.debug("List by userId='{}'", userId);
        final long count = oauthTokenRepository.countByOauthUserId(userId);
        final List<OauthToken> resultList = oauthTokenRepository.listByOauthUserId(userId, pageable);
        return new PageImpl<>(resultList, pageable, count);
    }

    @Override
    public Page<OauthToken> listByOrganizationId(final String organizationId, final Pageable pageable) {
        LOGGER.debug("List by organizationId='{}'", organizationId);
        final long count = oauthTokenRepository.countByOrganizationId(organizationId);
        final List<OauthToken> resultList = oauthTokenRepository.listByOrganizationId(organizationId, pageable);
        return new PageImpl<>(resultList, pageable, count);
    }

    @Override
    @Cacheable(key = "#hash", sync = true)
    public Optional<OauthToken> getByHash(final String hash) {
        LOGGER.debug("Fetching by hash='{}'", hash);
        return oauthTokenRepository.findByHash(hash);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(key = "#id"),
            @CacheEvict(key = "#hash")
    })
    public void deleteById(final String id, final String hash) {
        LOGGER.debug("Removing by id='{}'", id);
        oauthTokenRepository.deleteById(id);
    }

    @Override
    public void updateLinkedTokenId(final String id, final String linkedTokenId) {
        LOGGER.debug("Updating linked id for token id='{}'", id);
        oauthTokenRepository.updateLinkedTokenId(id, linkedTokenId);
    }
}
