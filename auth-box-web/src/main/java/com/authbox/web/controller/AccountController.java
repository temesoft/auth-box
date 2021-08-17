package com.authbox.web.controller;

import com.authbox.base.exception.AccessDeniedException;
import com.authbox.base.exception.BadRequestException;
import com.authbox.base.model.User;
import com.authbox.web.config.Constants;
import com.authbox.web.model.CreateAccountRequest;
import com.authbox.web.model.DeleteAccountsRequest;
import com.authbox.web.model.PasswordChangeRequest;
import com.google.common.collect.ImmutableList;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Consumer;

import static java.util.UUID.randomUUID;
import static org.springframework.util.ObjectUtils.isEmpty;

@RestController
@RequestMapping(Constants.API_PREFIX + "/account")
public class AccountController extends BaseController {

    private final Clock defaultClock;
    private final PasswordEncoder passwordEncoder;

    public AccountController(final Clock defaultClock, final PasswordEncoder passwordEncoder) {
        this.defaultClock = defaultClock;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    @PreAuthorize("hasRole('USER') || hasRole('ADMIN')")
    public User getCurrentAccountDetails() {
        return getCurrentUser();
    }

    @PostMapping
    @PreAuthorize("hasRole('USER') || hasRole('ADMIN')")
    public User updateCurrentAccount(@RequestBody final User updatedUser) {
        final User user = getCurrentUser();
        userDao.update(user.id, user.username, updatedUser.name, user.password, updatedUser.enabled, Instant.now(defaultClock));
        return userDao.getById(user.id).orElseThrow(() -> new BadRequestException("User not found by id: " + user.id));
    }

    @PostMapping("/password")
    @PreAuthorize("hasRole('USER') || hasRole('ADMIN')")
    public User updateCurrentAccountPassword(@RequestBody final PasswordChangeRequest passwordChangeRequest) {
        final User user = getCurrentUser();
        // verify new password min length
        if (isEmpty(passwordChangeRequest.oldPassword)) {
            throw new BadRequestException("Old password can not be empty");
        }
        // verify current password
        if (!passwordEncoder.matches(passwordChangeRequest.oldPassword, user.password)) {
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
        userDao.update(user.id, user.username, user.name, passwordEncoder.encode(passwordChangeRequest.newPassword), user.enabled, Instant.now(defaultClock));
        return userDao.getById(user.id).orElseThrow(() -> new BadRequestException("User not found by id: " + user.id));
    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<User> listAccounts(@RequestParam(value = "pageSize", defaultValue = "10") final int pageSize,
                                   @RequestParam(value = "currentPage", defaultValue = "0") final int currentPage) {
        final User adminUser = getCurrentUserVerifyAdmin();
        return userDao.listByOrganizationId(adminUser.organizationId, PageRequest.of(currentPage, pageSize));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public User getAccount(@PathVariable("id") final String id) {
        final User adminUser = getCurrentUserVerifyAdmin();
        final Optional<User> foundUser = userDao.getById(id);
        if (foundUser.isEmpty()) {
            throw new BadRequestException("Account not found by id: " + id);
        }
        if (!adminUser.organizationId.equals(foundUser.get().organizationId)) {
            throw new AccessDeniedException();
        }
        return foundUser.get();
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public User createAccountForOrganization(@RequestBody final CreateAccountRequest request) {
        final User adminUser = getCurrentUserVerifyAdmin();
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
        final String userId = randomUUID().toString();
        final Instant now = Instant.now(defaultClock);
        userDao.insert(
                new User(
                        userId,
                        now,
                        request.username.trim(),
                        (isEmpty(request.password) ? passwordEncoder.encode(randomUUID().toString()) : passwordEncoder.encode(request.password.trim())),
                        request.name.trim(),
                        ImmutableList.of(request.role.name()),
                        true,
                        adminUser.organizationId,
                        now
                )
        );
        return userDao.getById(userId).orElseThrow(() -> new BadRequestException("User not found by id: " + userId));
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> updateAccount(@PathVariable("id") final String id,
                                                @RequestBody final User updatedUser) {
        final User adminUser = getCurrentUserVerifyAdmin();
        final Optional<User> user = userDao.getById(updatedUser.id);
        if (user.isEmpty()) {
            throw new BadRequestException("User not found by id: " + id);
        }
        if (!adminUser.organizationId.equals(user.get().organizationId)) {
            throw new AccessDeniedException();
        }


        if (isEmpty(updatedUser.username)) {
            throw new BadRequestException("Account username can not be empty");
        }
        if (isEmpty(updatedUser.name)) {
            throw new BadRequestException("Account name can not be empty");
        }
        if (isEmpty(updatedUser.roles)) {
            throw new BadRequestException("Account access roles can not be empty");
        }
        if (!user.get().username.equals(updatedUser.username.trim())) {
            if (userDao.getByUsername(updatedUser.username.trim()).isPresent()) {
                throw new BadRequestException("This username is taken, please select another user");
            }
        }
        userDao.update(
                updatedUser.id,
                updatedUser.username.trim(),
                updatedUser.name.trim(),
                (isEmpty(updatedUser.password) ? passwordEncoder.encode(randomUUID().toString()) : passwordEncoder.encode(updatedUser.password.trim())),
                updatedUser.enabled,
                Instant.now(defaultClock)
        );

        return ResponseEntity.ok("{}");
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteAccounts(@RequestBody final DeleteAccountsRequest request) {
        final User adminUser = getCurrentUserVerifyAdmin();
        request.accountIds.stream().parallel().forEach(new Consumer<String>() {
            @Override
            public void accept(final String accountId) {
                final Optional<User> foundUser = userDao.getById(accountId);
                if (foundUser.isEmpty()) {
                    throw new BadRequestException("User not found by id: " + accountId);
                }
                if (!adminUser.organizationId.equals(foundUser.get().organizationId)) {
                    throw new AccessDeniedException();
                }
                if (adminUser.id.equals(foundUser.get().id)) {
                    throw new BadRequestException("User is unable to remove self");
                }
                userDao.delete(foundUser.get());
            }
        });
        return ResponseEntity.ok("{}");
    }
}
