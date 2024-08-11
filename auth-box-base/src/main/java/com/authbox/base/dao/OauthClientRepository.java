package com.authbox.base.dao;

import com.authbox.base.model.OauthClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OauthClientRepository extends CrudRepository<OauthClient, String> {

    Page<OauthClient> findAllByOrganizationId(String organizationId, Pageable pageable);
}
