package com.authbox.base.dao;

import com.authbox.base.model.OauthScope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OauthScopeRepository extends CrudRepository<OauthScope, String> {

    @Query("SELECT o FROM OauthScope o WHERE organizationId = ?1 ORDER BY createTime DESC")
    Page<OauthScope> listByOrganizationId(String organizationId, Pageable pageable);

    @Query("SELECT count(o.id) FROM OauthScope o WHERE organizationId = ?1 AND scope = ?2 ORDER BY createTime DESC")
    long countByOrganizationIdAndScope(String organizationId, String scope);

    @Modifying
    @Query("UPDATE OauthScope o SET o.scope = :scope, o.description = :description WHERE o.id = :id")
    void updateScopeAndDescription(String id, String scope, String description);

}
