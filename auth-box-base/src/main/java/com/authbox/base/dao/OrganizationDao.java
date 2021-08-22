package com.authbox.base.dao;

import com.authbox.base.model.Organization;

import java.time.Instant;
import java.util.Optional;

public interface OrganizationDao {

    void insert(Organization organization);

    Optional<Organization> getById(String id);

    Optional<Organization> getByDomainPrefix(String domainPrefix);

    void update(String id, String name, String domainPrefix, String address, boolean enabled, Instant lastUpdated);

}
