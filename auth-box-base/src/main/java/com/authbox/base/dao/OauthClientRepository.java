package com.authbox.base.dao;

import com.authbox.base.model.OauthClient;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OauthClientRepository extends CrudRepository<OauthClient, String> {

    long countByOrganizationId(String organizationId);

    @Query("SELECT o FROM OauthClient o WHERE organization_id = ?1 ORDER BY description ASC")
    List<OauthClient> listByOrganizationId(String organizationId, Pageable pageable);

}
