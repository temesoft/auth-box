package com.authbox.base.dao;

import com.authbox.base.config.CacheNamesConfiguration;
import com.authbox.base.model.OauthUser;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Optional;

@CacheConfig(cacheNames = CacheNamesConfiguration.CACHE_OAUTH_USER)
@AllArgsConstructor
@Slf4j
public class OauthUserDaoImpl implements OauthUserDao {

    private final OauthUserRepository oauthUserRepository;

    @Override
    public void insert(final OauthUser oauthUser) {
        log.debug("Inserting: {}", oauthUser);
        oauthUserRepository.save(oauthUser);
    }

    @Override
    @Cacheable(key = "#id", sync = true)
    public Optional<OauthUser> getById(final String id) {
        log.debug("Fetching by id='{}'", id);
        return oauthUserRepository.findById(id);
    }

    @Override
    public Optional<OauthUser> getByUsernameAndOrganizationId(final String username, final String organizationId) {
        log.debug("Fetching by username='{}' and organizationId='{}'", username, organizationId);
        return oauthUserRepository.findByUsernameAndOrganizationId(username, organizationId);
    }

    @Override
    public Page<OauthUser> listByOrganizationId(final String organizationId, final Pageable pageable) {
        log.debug("List by organizationId='{}'", organizationId);
        return oauthUserRepository.findByOrganizationId(organizationId, pageable);
    }

    @Override
    @CacheEvict(key = "#id")
    @Transactional
    public void deleteById(final String id) {
        log.debug("Fetching by id='{}'", id);
        oauthUserRepository.deleteById(id);
    }

    @Override
    @CacheEvict(key = "#id")
    @Transactional
    public void update(final String id,
                       final String username,
                       final String password,
                       final boolean enabled,
                       final String metadata,
                       final boolean using2Fa,
                       final Instant lastUpdated) {
        log.debug("Updating by id='{}', username='{}'", id, username);
        oauthUserRepository.update(id, username, password, enabled, metadata, using2Fa, lastUpdated);
    }
}
