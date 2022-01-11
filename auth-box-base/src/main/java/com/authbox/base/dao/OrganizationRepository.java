package com.authbox.base.dao;

import com.authbox.base.model.Organization;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface OrganizationRepository extends CrudRepository<Organization, String> {

    Optional<Organization> findByDomainPrefix(String domainPrefix);

    @Modifying
    @Query("update Organization o set o.name = :name, o.domainPrefix = :domainPrefix, o.address = :address, o.enabled = :enabled, o.logoUrl = :logoUrl, o.lastUpdated = :lastUpdated WHERE o.id = :id")
    void update(String id, String name, String domainPrefix, String address, boolean enabled, String logoUrl, Instant lastUpdated);

}
