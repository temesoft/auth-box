package com.authbox.base.dao;

import com.authbox.base.model.OauthClientScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class OauthClientScopeDaoImpl implements OauthClientScopeDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(OauthClientScopeDaoImpl.class);

    private final OauthClientScopeRepository oauthClientScopeRepository;

    public OauthClientScopeDaoImpl(final OauthClientScopeRepository oauthClientScopeRepository) {
        this.oauthClientScopeRepository = oauthClientScopeRepository;
    }

    @Override
    public void insert(final OauthClientScope oauthClientScope) {
        LOGGER.debug("Inserting: {}", oauthClientScope);
        oauthClientScopeRepository.save(oauthClientScope);
    }

    @Override
    public Optional<OauthClientScope> getById(final String id) {
        LOGGER.debug("Fetching by id='{}'", id);
        return oauthClientScopeRepository.findById(id);
    }

    @Override
    public List<OauthClientScope> listByClientId(final String clientId) {
        LOGGER.debug("List by clientId='{}'", clientId);
        return oauthClientScopeRepository.listByClientId(clientId);
    }

    @Override
    public long countByScopeIds(final List<String> scopeIds) {
        LOGGER.debug("Count by scopeIds={}", scopeIds);
        return oauthClientScopeRepository.countByScopeIds(scopeIds);
    }

    @Override
    public void deleteById(final String id) {
        LOGGER.debug("Delete by id='{}'", id);
        oauthClientScopeRepository.deleteById(id);
    }

    @Override
    public void deleteByClientIdAndScopeId(final String clientId, final String scopeId) {
        LOGGER.debug("Delete by clientId='{}' and scopeId='{}'", clientId, scopeId);
        oauthClientScopeRepository.deleteByClientIdAndScopeId(clientId, scopeId);
    }

    @Override
    public void deleteByScopeId(final String scopeId) {
        LOGGER.debug("Delete by scopeId='{}'", scopeId);
        oauthClientScopeRepository.deleteByScopeId(scopeId);
    }
}
