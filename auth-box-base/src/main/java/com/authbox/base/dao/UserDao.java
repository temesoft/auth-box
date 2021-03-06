package com.authbox.base.dao;

import com.authbox.base.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserDao {

    void insert(User user);

    Optional<User> getById(String id);

    Optional<User> getByUsername(String username);

    void update(String userId, String username, String name, String password, boolean enabled, Instant lastUpdated);

    Page<User> listByOrganizationId(String organizationId, Pageable pageable);

    void delete(User user);

}
