package com.authbox.base.dao;

import com.authbox.base.model.OauthScope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface OauthScopeDao {

    void insert(OauthScope oauthScope);

    Optional<OauthScope> getById(String id);

    List<OauthScope> listByIds(List<String> ids);

    Page<OauthScope> listByOrganizationId(String organizationId, Pageable pageable);

    boolean existsByOrganizationIdAndScope(String organizationId, String scope);

    void deleteById(String id);

    void update(String id, String scope, String description);

}
