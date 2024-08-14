package com.authbox.web.service;

import com.authbox.base.dao.UserDao;
import com.authbox.base.exception.AccessDeniedException;
import com.authbox.base.exception.BadRequestException;
import com.authbox.base.exception.EntityNotFoundException;
import com.authbox.base.model.User;
import com.authbox.web.model.CreateAccountRequest;
import com.authbox.web.model.DeleteAccountsRequest;
import com.authbox.web.model.PasswordChangeRequest;
import com.authbox.web.model.UpdateUserRequest;
import com.authbox.web.model.UserDto;
import lombok.AllArgsConstructor;
import lombok.val;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static com.authbox.base.util.IdUtils.createId;
import static org.springframework.util.ObjectUtils.isEmpty;

@Service
@AllArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final Clock defaultClock;
    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;

    /**
     * Updates user entity using UpdateUserRequest object
     */
    @Override
    public UserDto updateCurrentAccount(final UserDto currentUser, final UpdateUserRequest updatedUser) {
        userDao.update(
                currentUser.getId(),
                currentUser.getUsername(),
                updatedUser.getName(),
                currentUser.getPassword(),
                updatedUser.isEnabled(),
                Instant.now(defaultClock)
        );
        return UserDto.fromEntity(userDao.getById(currentUser.getId()).orElseThrow(userNotFound(currentUser.getId())));
    }

    /**
     * Updates password for current user using PasswordChangeRequest object
     */
    @Override
    public UserDto updateCurrentAccountPassword(final UserDto currentUser, final PasswordChangeRequest passwordChangeRequest) {
        // verify new password min length
        if (isEmpty(passwordChangeRequest.oldPassword)) {
            throw new BadRequestException("Old password can not be empty");
        }
        // verify current password
        if (!passwordEncoder.matches(passwordChangeRequest.oldPassword, currentUser.getPassword())) {
            throw new BadRequestException("Old password does not match");
        }
        // verify new password min length
        if (isEmpty(passwordChangeRequest.newPassword) || passwordChangeRequest.newPassword.length() < 6) {
            throw new BadRequestException("New password is empty or shorter than 6 characters");
        }
        // verify new password and password2 match
        if (!passwordChangeRequest.newPassword.equals(passwordChangeRequest.newPassword2)) {
            throw new BadRequestException("New password and new password 2 do not match");
        }
        userDao.update(
                currentUser.getId(),
                currentUser.getUsername(),
                currentUser.getName(),
                passwordEncoder.encode(passwordChangeRequest.newPassword),
                currentUser.isEnabled(),
                Instant.now(defaultClock)
        );
        return UserDto.fromEntity(userDao.getById(currentUser.getId()).orElseThrow(userNotFound(currentUser.getId())));
    }

    /**
     * Returns paginated list of users
     */
    @Override
    public Page<UserDto> listAccounts(final UserDto adminUser, final int pageSize, final int currentPage) {
        return userDao.listByOrganizationId(adminUser.getOrganizationId(), PageRequest.of(currentPage, pageSize))
                .map(UserDto::fromEntity);
    }

    /**
     * Returns user by id
     */
    @Override
    public UserDto getAccount(final UserDto adminUser, final String id) {
        val foundUser = userDao.getById(id);
        if (foundUser.isEmpty()) {
            throwUserNotFound(id);
        }
        if (!adminUser.getOrganizationId().equals(foundUser.get().getOrganizationId())) {
            throw new AccessDeniedException();
        }
        return UserDto.fromEntity(foundUser.get());
    }

    /**
     * Create new user for organization
     */
    @Override
    public UserDto createAccountForOrganization(final UserDto adminUser, final CreateAccountRequest request) {
        if (isEmpty(request.username)) {
            throw new BadRequestException("Account username can not be empty");
        }
        if (isEmpty(request.name)) {
            throw new BadRequestException("Account name can not be empty");
        }
        if (request.role == null) {
            throw new BadRequestException("Account access roles can not be empty");
        }
        if (userDao.getByUsername(request.username.trim()).isPresent()) {
            throw new BadRequestException("This username is taken, please select another user");
        }
        val userId = createId();
        val now = Instant.now(defaultClock);

        val password = (isEmpty(request.password) ?
                passwordEncoder.encode(createId())
                : passwordEncoder.encode(request.password.trim()));

        userDao.insert(
                new User(
                        userId,
                        now,
                        request.username.trim(),
                        password,
                        request.name.trim(),
                        List.of(request.role.name()),
                        true,
                        adminUser.getOrganizationId(),
                        now
                )
        );
        return UserDto.fromEntity(userDao.getById(userId).orElseThrow(userNotFound(userId)));
    }

    /**
     * Updates user by id using UpdateUserRequest object
     */
    @Override
    public void updateAccount(final UserDto adminUser, final String id, final UpdateUserRequest updatedUser) {
        val user = userDao.getById(updatedUser.getId());
        if (user.isEmpty()) {
            throwUserNotFound(id);
        }
        if (!adminUser.getOrganizationId().equals(user.get().getOrganizationId())) {
            throw new AccessDeniedException();
        }
        if (isEmpty(updatedUser.getUsername())) {
            throw new BadRequestException("Account username can not be empty");
        }
        if (isEmpty(updatedUser.getName())) {
            throw new BadRequestException("Account name can not be empty");
        }
        if (isEmpty(updatedUser.getRoles())) {
            throw new BadRequestException("Account access roles can not be empty");
        }
        if (!user.get().getUsername().equals(updatedUser.getUsername().trim())) {
            if (userDao.getByUsername(updatedUser.getUsername().trim()).isPresent()) {
                throw new BadRequestException("This username is taken, please select another user");
            }
        }

        val password = (isEmpty(updatedUser.getPassword()) ?
                passwordEncoder.encode(createId())
                : passwordEncoder.encode(updatedUser.getPassword().trim()));

        userDao.update(
                updatedUser.getId(),
                updatedUser.getUsername().trim(),
                updatedUser.getName().trim(),
                password,
                updatedUser.isEnabled(),
                Instant.now(defaultClock)
        );
    }

    /**
     * Deletes user using DeleteAccountsRequest.accountIds
     */
    @Override
    public void deleteAccounts(final UserDto adminUser, final DeleteAccountsRequest request) {
        request.accountIds.stream().parallel().forEach(accountId -> {
            final Optional<User> foundUser = userDao.getById(accountId);
            if (foundUser.isEmpty()) {
                throw new BadRequestException("User not found by id: " + accountId);
            }
            if (!adminUser.getOrganizationId().equals(foundUser.get().getOrganizationId())) {
                throw new AccessDeniedException();
            }
            if (adminUser.getId().equals(foundUser.get().getId())) {
                throw new BadRequestException("User is unable to remove self");
            }
            userDao.delete(foundUser.get());
        });
    }

    private void throwUserNotFound(final String userId) {
        throw new EntityNotFoundException("User not found by id: " + userId);
    }

    private Supplier<EntityNotFoundException> userNotFound(final String userId) {
        return () -> new EntityNotFoundException("User not found by id: " + userId);
    }
}
