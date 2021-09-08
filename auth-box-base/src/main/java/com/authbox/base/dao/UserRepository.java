package com.authbox.base.dao;

import com.authbox.base.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends CrudRepository<User, String> {

    Optional<User> findByUsername(String username);

    @Modifying
    @Query("update User o set o.username = :username, o.name = :name, o.password = :password, o.enabled = :enabled, o.lastUpdated = :lastUpdated WHERE o.id = :id")
    void update(String id, String username, String name, String password, boolean enabled, Instant lastUpdated);

    long countByOrganizationId(String organizationId);

    @Query("SELECT o FROM User o WHERE organization_id = ?1 ORDER BY name ASC")
    List<User> listByOrganizationId(String organizationId, Pageable pageable);


}
