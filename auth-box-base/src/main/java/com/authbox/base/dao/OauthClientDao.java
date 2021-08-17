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

    int insert(OauthClient oauthClient);

    Optional<OauthClient> getById(String id);

    Page<OauthClient> listByOrganizationId(String organizationId, Pageable pageable);

    int updateById(final String id,
                   final String description,
                   final String grantTypesCsv,
                   final boolean enabled,
                   final String redirectUrlsCsv,
                   final Duration expiration,
                   final Duration refreshExpiration,
                   final TokenFormat tokenFormat,
                   final String privateKey,
                   final String publicKey,
                   final Instant lastUpdated);

    int deleteById(String id);

}
