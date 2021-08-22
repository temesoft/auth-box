package com.authbox.base.dao;

import com.authbox.base.model.OauthClientScope;

import java.util.List;
import java.util.Optional;

public interface OauthClientScopeDao {

    void insert(OauthClientScope oauthClientScope);

    Optional<OauthClientScope> getById(String id);

    List<OauthClientScope> listByClientId(String clientId);

    long countByScopeIds(List<String> scopeIds);

    void deleteById(String id);

    void deleteByClientIdAndScopeId(String clientId, String scopeId);

    void deleteByScopeId(String scopeId);

}
