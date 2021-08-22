package com.authbox.base.dao;

import com.authbox.base.model.OauthClientScope;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OauthClientScopeRepository extends CrudRepository<OauthClientScope, String> {

    @Query("SELECT o FROM OauthClientScope o WHERE clientId = ?1 ORDER BY o.createTime DESC")
    List<OauthClientScope> listByClientId(String clientId);

    @Query("SELECT count(o.id) FROM OauthClientScope o WHERE o.scopeId in ?1 ORDER BY o.createTime DESC")
    long countByScopeIds(List<String> scopeIds);

    @Modifying
    @Query("DELETE FROM OauthClientScope o WHERE o.scopeId = :scopeId")
    void deleteByScopeId(String scopeId);

    @Modifying
    @Query("DELETE FROM OauthClientScope o WHERE o.clientId = :clientId AND o.scopeId = :scopeId")
    void deleteByClientIdAndScopeId(String clientId, String scopeId);

}
