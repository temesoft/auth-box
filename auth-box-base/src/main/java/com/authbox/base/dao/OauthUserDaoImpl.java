package com.authbox.base.dao;

import com.authbox.base.config.CacheNamesConfiguration;
import com.authbox.base.model.OauthUser;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Optional;

@CacheConfig(cacheNames = CacheNamesConfiguration.CACHE_OAUTH_USER)
public class OauthUserDaoImpl implements OauthUserDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(OauthUserDaoImpl.class);

    private final OauthUserRepository oauthUserRepository;

    public OauthUserDaoImpl(final OauthUserRepository oauthUserRepository) {
        this.oauthUserRepository = oauthUserRepository;
    }

    @Override
    public void insert(final OauthUser oauthUser) {
        LOGGER.debug("Inserting: {}", oauthUser);
        oauthUserRepository.save(oauthUser);
    }

    @Override
    @Cacheable(key = "#id", sync = true)
    public Optional<OauthUser> getById(final String id) {
        LOGGER.debug("Fetching by id='{}'", id);
        return oauthUserRepository.findById(id);
    }

    @Override
    public Optional<OauthUser> getByUsernameAndOrganizationId(final String username, final String organizationId) {
        LOGGER.debug("Fetching by username='{}' and organizationId='{}'", username, organizationId);
        return oauthUserRepository.findByUsernameAndOrganizationId(username, organizationId);
    }

    @Override
    public Page<OauthUser> listByOrganizationId(final String organizationId, final Pageable pageable) {
        LOGGER.debug("List by organizationId='{}'", organizationId);
        return oauthUserRepository.findByOrganizationId(organizationId, pageable);
    }

    @Override
    @CacheEvict(key = "#id")
    @Transactional
    public void deleteById(final String id) {
        LOGGER.debug("Fetching by id='{}'", id);
        oauthUserRepository.deleteById(id);
    }

    @Override
    @CacheEvict(key = "#id")
    @Transactional
    public void update(final String id, final String username, final String password, final boolean enabled, final String metadata, final boolean using2Fa, final Instant lastUpdated) {
        LOGGER.debug("Updating by id='{}', username='{}'", id, username);
        oauthUserRepository.update(id, username, password, enabled, metadata, using2Fa, lastUpdated);
    }
}
