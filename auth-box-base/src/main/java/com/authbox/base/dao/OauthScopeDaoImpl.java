package com.authbox.base.dao;

import com.authbox.base.config.CacheNamesConfiguration;
import com.authbox.base.model.OauthScope;
import com.google.common.collect.ImmutableList;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@CacheConfig(cacheNames = CacheNamesConfiguration.CACHE_OAUTH_SCOPE)
@AllArgsConstructor
@Slf4j
public class OauthScopeDaoImpl implements OauthScopeDao {

    private final OauthScopeRepository oauthScopeRepository;

    @Override
    public void insert(final OauthScope oauthScope) {
        log.debug("Inserting: {}", oauthScope);
        oauthScopeRepository.save(oauthScope);
    }

    @Override
    @Cacheable(key = "#id", sync = true)
    public Optional<OauthScope> getById(final String id) {
        log.debug("Fetching by id='{}'", id);
        return oauthScopeRepository.findById(id);
    }

    @Override
    public List<OauthScope> listByIds(final List<String> ids) {
        log.debug("List by ids={}", ids);
        return ImmutableList.copyOf(oauthScopeRepository.findAllById(ids));
    }

    @Override
    public Page<OauthScope> listByOrganizationId(final String organizationId, final Pageable pageable) {
        log.debug("List by organizationId={}", organizationId);
        return oauthScopeRepository.listByOrganizationId(organizationId, pageable);
    }

    @Override
    public boolean existsByOrganizationIdAndScope(final String organizationId, final String scope) {
        log.debug("Exists by organizationId='{}' and scope={}", organizationId, scope);
        return oauthScopeRepository.countByOrganizationIdAndScope(organizationId, scope) > 0;
    }

    @Override
    @CacheEvict(key = "#id")
    public void deleteById(final String id) {
        log.debug("Delete by id={}", id);
        oauthScopeRepository.deleteById(id);
    }

    @Override
    @CacheEvict(key = "#id")
    public void update(final String id, final String scope, final String description) {
        log.debug("Update by id={}", id);
        oauthScopeRepository.updateScopeAndDescription(id, scope, description);
    }
}
