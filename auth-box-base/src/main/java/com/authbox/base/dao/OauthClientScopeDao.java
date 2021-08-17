package com.authbox.base.dao;

import com.authbox.base.model.OauthClientScope;

import java.util.List;
import java.util.Optional;

public interface OauthClientScopeDao {

    int insert(OauthClientScope oauthClientScope);

    Optional<OauthClientScope> getById(String id);

    List<OauthClientScope> listByClientId(String clientId);

    int countByScopeIds(List<String> scopeIds);

    int deleteById(String id);

    int deleteByClientIdAndScopeId(String clientId, String scopeId);

    int deleteByScopeId(String scopeId);

}
