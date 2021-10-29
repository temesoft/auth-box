package com.authbox.base.dao;

import com.authbox.base.config.CacheNamesConfiguration;
import com.authbox.base.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@CacheConfig(cacheNames = CacheNamesConfiguration.CACHE_USER)
public class UserDaoImpl implements UserDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserDaoImpl.class);

    private final UserRepository userRepository;

    public UserDaoImpl(final UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void insert(final User user) {
        LOGGER.debug("Inserting: {}", user);
        userRepository.save(user);
    }

    @Override
    @Cacheable(key = "#id", sync = true)
    public Optional<User> getById(final String id) {
        LOGGER.debug("Fetching by id='{}'", id);
        return userRepository.findById(id);
    }

    @Override
    @Cacheable(key = "#username", sync = true)
    public Optional<User> getByUsername(final String username) {
        LOGGER.debug("Fetching  by username='{}'", username);
        return userRepository.findByUsername(username);
    }

    @Override
    public Page<User> listByOrganizationId(final String organizationId, final Pageable pageable) {
        final long count = userRepository.countByOrganizationId(organizationId);
        final List<User> resultList = userRepository.listByOrganizationId(organizationId, pageable);
        return new PageImpl<>(resultList, pageable, count);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(key = "#user.id"),
            @CacheEvict(key = "#user.username")
    })
    public void delete(final User user) {
        LOGGER.debug("Removing token by id='{}'", user.getId());
        userRepository.deleteById(user.getId());
    }

    @Override
    @Caching(evict = {
            @CacheEvict(key = "#id"),
            @CacheEvict(key = "#username")
    })
    public void update(final String userId, final String username, final String name, final String password, final boolean enabled, final Instant lastUpdated) {
        LOGGER.debug("Fetching id='{}', username='{}'", userId, username);
        userRepository.update(userId, username, name, password, enabled, lastUpdated);
    }
}
