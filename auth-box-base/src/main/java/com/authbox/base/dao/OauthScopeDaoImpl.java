package com.authbox.base.dao;

import com.authbox.base.config.CacheNamesConfiguration;
import com.authbox.base.model.OauthScope;
import com.google.common.collect.ImmutableList;
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

@CacheConfig(cacheNames = CacheNamesConfiguration.CACHE_OAUTH_SCOPE)
public class OauthScopeDaoImpl implements OauthScopeDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(OauthScopeDaoImpl.class);

    private final OauthScopeRepository oauthScopeRepository;

    public OauthScopeDaoImpl(final OauthScopeRepository oauthScopeRepository) {
        this.oauthScopeRepository = oauthScopeRepository;
    }

    @Override
    public void insert(final OauthScope oauthScope) {
        LOGGER.debug("Inserting: {}", oauthScope);
        oauthScopeRepository.save(oauthScope);
    }

    @Override
    @Cacheable(key = "#id", sync = true)
    public Optional<OauthScope> getById(final String id) {
        LOGGER.debug("Fetching by id='{}'", id);
        return oauthScopeRepository.findById(id);
    }

    @Override
    public List<OauthScope> listByIds(final List<String> ids) {
        LOGGER.debug("List by ids={}", ids);
        return ImmutableList.copyOf(oauthScopeRepository.findAllById(ids));
    }

    @Override
    public Page<OauthScope> listByOrganizationId(final String organizationId, final Pageable pageable) {
        LOGGER.debug("List by organizationId={}", organizationId);
        return oauthScopeRepository.listByOrganizationId(organizationId, pageable);
    }

    @Override
    public boolean existsByOrganizationIdAndScope(final String organizationId, final String scope) {
        LOGGER.debug("Exists by organizationId='{}' and scope={}", organizationId, scope);
        final long count = oauthScopeRepository.countByOrganizationIdAndScope(organizationId, scope);
        return count > 0;
    }

    @Override
    @CacheEvict(key = "#id")
    public void deleteById(final String id) {
        LOGGER.debug("Delete by id={}", id);
        oauthScopeRepository.deleteById(id);
    }

    @Override
    @CacheEvict(key = "#id")
    public void update(final String id, final String scope, final String description) {
        LOGGER.debug("Update by id={}", id);
        // final String id, final String scope, final String description
        oauthScopeRepository.updateScopeAndDescription(id, scope, description);
    }
}
