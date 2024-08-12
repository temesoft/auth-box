package com.authbox.base.dao;

import com.authbox.base.config.CacheNamesConfiguration;
import com.authbox.base.model.User;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Optional;

@CacheConfig(cacheNames = CacheNamesConfiguration.CACHE_USER)
@AllArgsConstructor
@Slf4j
public class UserDaoImpl implements UserDao {

    private final UserRepository userRepository;

    @Override
    public void insert(final User user) {
        log.debug("Inserting: {}", user);
        userRepository.save(user);
    }

    @Override
    @Cacheable(key = "#id", sync = true)
    public Optional<User> getById(final String id) {
        log.debug("Fetching by id='{}'", id);
        return userRepository.findById(id);
    }

    @Override
    @Cacheable(key = "#username", sync = true)
    public Optional<User> getByUsername(final String username) {
        log.debug("Fetching  by username='{}'", username);
        return userRepository.findByUsername(username);
    }

    @Override
    public Page<User> listByOrganizationId(final String organizationId, final Pageable pageable) {
        return userRepository.listByOrganizationId(organizationId, pageable);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(key = "#user.id"),
            @CacheEvict(key = "#user.username")
    })
    public void delete(final User user) {
        log.debug("Removing token by id='{}'", user.getId());
        userRepository.deleteById(user.getId());
    }

    @Override
    @Caching(evict = {
            @CacheEvict(key = "#id"),
            @CacheEvict(key = "#username")
    })
    public void update(final String userId,
                       final String username,
                       final String name,
                       final String password,
                       final boolean enabled,
                       final Instant lastUpdated) {
        log.debug("Fetching id='{}', username='{}'", userId, username);
        userRepository.update(userId, username, name, password, enabled, lastUpdated);
    }
}
