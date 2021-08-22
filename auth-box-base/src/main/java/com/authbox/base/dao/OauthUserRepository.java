package com.authbox.base.dao;

import com.authbox.base.model.OauthUser;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface OauthUserRepository extends CrudRepository<OauthUser, String> {

    Optional<OauthUser> findByUsernameAndOrganizationId(String username, String organizationId);

    long countByOrganizationId(String organizationId);

    @Query("SELECT o FROM OauthUser o WHERE organization_id = ?1 ORDER BY create_time DESC")
    List<OauthUser> listByOrganizationId(final String organizationId, Pageable pageable);

    @Modifying
    @Query("update OauthUser o set o.username = :username, o.password = :password, o.enabled = :enabled, o.metadata = :metadata, o.using2Fa = :using2Fa, o.lastUpdated = :lastUpdated WHERE o.id = :id")
    void update(final String id, final String username, final String password, final boolean enabled, final String metadata, final boolean using2Fa, final Instant lastUpdated);

}
