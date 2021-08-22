package com.authbox.base.dao;

import com.authbox.base.model.OauthScope;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OauthScopeRepository extends CrudRepository<OauthScope, String> {

    long countByOrganizationId(String organizationId);

    @Query("SELECT o FROM OauthScope o WHERE organization_id = ?1 ORDER BY create_time DESC")
    List<OauthScope> listByOrganizationId(String organizationId, Pageable pageable);

    @Query("SELECT o FROM OauthScope o WHERE client_id = ?1 ORDER BY create_time DESC")
    List<OauthScope> listByClientId(String clientId);

    @Query("SELECT count(o.id) FROM OauthScope o WHERE organization_id = ?1 AND scope = ?2 ORDER BY create_time DESC")
    long countByOrganizationIdAndScope(String organizationId, String scope);

    @Modifying
    @Query("update OauthScope o set o.scope = :scope, o.description = :description WHERE o.id = :id")
    void updateScopeAndDescription(String id, String scope, String description);

}
