package com.authbox.base.dao;

import com.authbox.base.model.OauthToken;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OauthTokenRepository extends CrudRepository<OauthToken, String> {

    long countByClientId(String clientId);

    @Query("SELECT o FROM OauthToken o WHERE o.clientId = ?1 ORDER BY create_time DESC")
    List<OauthToken> listByClientId(final String clientId, Pageable pageable);

    long countByOauthUserId(String oauthUserId);

    @Query("SELECT o FROM OauthToken o WHERE o.oauthUserId = ?1 ORDER BY create_time DESC")
    List<OauthToken> listByOauthUserId(final String oauthUserId, Pageable pageable);

    long countByOrganizationId(String organizationId);

    @Query("SELECT o FROM OauthToken o WHERE o.organizationId = ?1 ORDER BY create_time DESC")
    List<OauthToken> listByOrganizationId(final String organizationId, Pageable pageable);

    Optional<OauthToken> findByHash(final String hash);

    @Modifying
    @Query("update OauthToken o set o.linkedTokenId = :linkedTokenId WHERE o.id = :id")
    void updateLinkedTokenId(String id, String linkedTokenId);
}
