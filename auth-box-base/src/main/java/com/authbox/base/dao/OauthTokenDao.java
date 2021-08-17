package com.authbox.base.dao;

import com.authbox.base.model.OauthToken;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface OauthTokenDao {

    int insert(OauthToken oauthToken);

    Optional<OauthToken> getById(String id);

    Page<OauthToken> listByClientId(String clientId, Pageable pageable);

    Page<OauthToken> listByUserId(String userId, Pageable pageable);

    Page<OauthToken> listByOrganizationId(String organizationId, Pageable pageable);

    Optional<OauthToken> getByHash(String hash);

    int deleteById(String id, String hash);

    int updateLinkedTokenId(String id, String linkedTokenId);

}
