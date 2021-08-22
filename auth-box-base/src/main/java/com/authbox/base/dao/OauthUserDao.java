package com.authbox.base.dao;

import com.authbox.base.model.OauthUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OauthUserDao {

    void insert(OauthUser oauthUser);

    Optional<OauthUser> getById(String id);

    Optional<OauthUser> getByUsernameAndOrganizationId(String username, String organizationId);

    Page<OauthUser> listByOrganizationId(String organizationId, Pageable pageable);

    void deleteById(String id);

    void update(String id, String username, String password, boolean enabled, String metadata, boolean using2Fa, Instant lastUpdated);

}
