package com.authbox.base.dao;

import com.authbox.base.model.OauthUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface OauthUserRepository extends CrudRepository<OauthUser, String> {

    Optional<OauthUser> findByUsernameAndOrganizationId(String username, String organizationId);

    @Query("SELECT o FROM OauthUser o WHERE o.organizationId = ?1 ORDER BY o.createTime DESC")
    Page<OauthUser> findByOrganizationId(String organizationId, Pageable pageable);

    @Modifying
    @Query("update OauthUser o set o.username = :username, o.password = :password, o.enabled = :enabled, o.metadata = :metadata, o.using2Fa = :using2Fa, o.lastUpdated = :lastUpdated WHERE o.id = :id")
    void update(String id, String username, String password, boolean enabled, String metadata, boolean using2Fa, Instant lastUpdated);

}
