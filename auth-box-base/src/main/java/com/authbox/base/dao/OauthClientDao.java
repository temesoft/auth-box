package com.authbox.base.dao;

import com.authbox.base.model.OauthClient;
import com.authbox.base.model.TokenFormat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OauthClientDao {

    void insert(OauthClient oauthClient);

    Optional<OauthClient> getById(String id);

    Page<OauthClient> listByOrganizationId(String organizationId, Pageable pageable);

    void update(OauthClient oauthClient);

    void deleteById(String id);

}
