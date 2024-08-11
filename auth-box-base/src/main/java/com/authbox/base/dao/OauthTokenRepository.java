package com.authbox.base.dao;

import com.authbox.base.model.OauthToken;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OauthTokenRepository extends CrudRepository<OauthToken, String> {

    @Query("SELECT o FROM OauthToken o WHERE o.clientId = ?1 ORDER BY createTime DESC")
    Page<OauthToken> listByClientId(final String clientId, Pageable pageable);

    @Query("SELECT o FROM OauthToken o WHERE o.oauthUserId = ?1 ORDER BY createTime DESC")
    Page<OauthToken> listByOauthUserId(final String oauthUserId, Pageable pageable);

    @Query("SELECT o FROM OauthToken o WHERE o.organizationId = ?1 ORDER BY o.createTime DESC")
    Page<OauthToken> listByOrganizationId(final String organizationId, Pageable pageable);

    Optional<OauthToken> findByHash(final String hash);

    @Modifying
    @Query("update OauthToken o set o.linkedTokenId = :linkedTokenId WHERE o.id = :id")
    void updateLinkedTokenId(String id, String linkedTokenId);
}
