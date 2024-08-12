package com.authbox.base.dao;

import com.authbox.base.model.OauthClientScope;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Slf4j
public class OauthClientScopeDaoImpl implements OauthClientScopeDao {

    private final OauthClientScopeRepository oauthClientScopeRepository;

    @Override
    public void insert(final OauthClientScope oauthClientScope) {
        log.debug("Inserting: {}", oauthClientScope);
        oauthClientScopeRepository.save(oauthClientScope);
    }

    @Override
    public Optional<OauthClientScope> getById(final String id) {
        log.debug("Fetching by id='{}'", id);
        return oauthClientScopeRepository.findById(id);
    }

    @Override
    public List<OauthClientScope> listByClientId(final String clientId) {
        log.debug("List by clientId='{}'", clientId);
        return oauthClientScopeRepository.listByClientId(clientId);
    }

    @Override
    public long countByScopeIds(final List<String> scopeIds) {
        log.debug("Count by scopeIds={}", scopeIds);
        return oauthClientScopeRepository.countByScopeIds(scopeIds);
    }

    @Override
    public void deleteById(final String id) {
        log.debug("Delete by id='{}'", id);
        oauthClientScopeRepository.deleteById(id);
    }

    @Override
    public void deleteByClientIdAndScopeId(final String clientId, final String scopeId) {
        log.debug("Delete by clientId='{}' and scopeId='{}'", clientId, scopeId);
        oauthClientScopeRepository.deleteByClientIdAndScopeId(clientId, scopeId);
    }

    @Override
    public void deleteByScopeId(final String scopeId) {
        log.debug("Delete by scopeId='{}'", scopeId);
        oauthClientScopeRepository.deleteByScopeId(scopeId);
    }
}
