package com.authbox.base.dao;

import com.authbox.base.model.OauthScope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface OauthScopeDao {

    int insert(OauthScope oauthScope);

    Optional<OauthScope> getById(String id);

    List<OauthScope> listByIds(List<String> ids);

    Page<OauthScope> listByOrganizationId(String organizationId, Pageable pageable);

    List<OauthScope> listByClientId(String clientId);

    boolean existsByOrganizationIdAndScope(String organizationId, String scope);

    int deleteById(String id);

    int updateById(String id, String scope, String description);

}
