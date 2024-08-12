package com.authbox.web.controller;

import com.authbox.base.exception.AccessDeniedException;
import com.authbox.base.exception.BadRequestException;
import com.authbox.base.model.User;
import com.authbox.web.config.Constants;
import com.authbox.web.model.CreateAccountRequest;
import com.authbox.web.model.DeleteAccountsRequest;
import com.authbox.web.model.PasswordChangeRequest;
import com.authbox.web.model.UpdateUserRequest;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.val;
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
import java.util.List;
import java.util.Optional;

import static java.util.UUID.randomUUID;
import static org.springframework.util.ObjectUtils.isEmpty;

@RestController
@RequestMapping(Constants.API_PREFIX + "/account")
@AllArgsConstructor
public class AccountController extends BaseController {

    private final Clock defaultClock;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    @PreAuthorize("hasRole('USER') || hasRole('ADMIN')")
    public User getCurrentAccountDetails() {
        return getCurrentUser();
    }

    @PostMapping
    @PreAuthorize("hasRole('USER') || hasRole('ADMIN')")
    @Transactional
    public User updateCurrentAccount(@RequestBody final UpdateUserRequest updatedUser) {
        val user = getCurrentUser();
        userDao.update(user.getId(), user.getUsername(), updatedUser.getName(), user.getPassword(), updatedUser.isEnabled(), Instant.now(defaultClock));
        return userDao.getById(user.getId()).orElseThrow(() -> new BadRequestException("User not found by id: " + user.getId()));
    }

    @PostMapping("/password")
    @PreAuthorize("hasRole('USER') || hasRole('ADMIN')")
    @Transactional
    public User updateCurrentAccountPassword(@RequestBody final PasswordChangeRequest passwordChangeRequest) {
        val user = getCurrentUser();
        // verify new password min length
        if (isEmpty(passwordChangeRequest.oldPassword)) {
            throw new BadRequestException("Old password can not be empty");
        }
        // verify current password
        if (!passwordEncoder.matches(passwordChangeRequest.oldPassword, user.getPassword())) {
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
                user.getId(),
                user.getUsername(),
                user.getName(),
                passwordEncoder.encode(passwordChangeRequest.newPassword),
                user.isEnabled(),
                Instant.now(defaultClock)
        );
        return userDao.getById(user.getId())
                .orElseThrow(() -> new BadRequestException("User not found by id: " + user.getId()));
    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<User> listAccounts(@RequestParam(value = "pageSize", defaultValue = "10") final int pageSize,
                                   @RequestParam(value = "currentPage", defaultValue = "0") final int currentPage) {
        val adminUser = getCurrentUserVerifyAdmin();
        return userDao.listByOrganizationId(adminUser.getOrganizationId(), PageRequest.of(currentPage, pageSize));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public User getAccount(@PathVariable("id") final String id) {
        val adminUser = getCurrentUserVerifyAdmin();
        val foundUser = userDao.getById(id);
        if (foundUser.isEmpty()) {
            throw new BadRequestException("Account not found by id: " + id);
        }
        if (!adminUser.getOrganizationId().equals(foundUser.get().getOrganizationId())) {
            throw new AccessDeniedException();
        }
        return foundUser.get();
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public User createAccountForOrganization(@RequestBody final CreateAccountRequest request) {
        val adminUser = getCurrentUserVerifyAdmin();
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
        val userId = randomUUID().toString();
        val now = Instant.now(defaultClock);

        val password = (isEmpty(request.password) ?
                passwordEncoder.encode(randomUUID().toString())
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
        return userDao.getById(userId)
                .orElseThrow(() -> new BadRequestException("User not found by id: " + userId));
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<String> updateAccount(@PathVariable("id") final String id,
                                                @RequestBody final UpdateUserRequest updatedUser) {
        val adminUser = getCurrentUserVerifyAdmin();
        val user = userDao.getById(updatedUser.getId());
        if (user.isEmpty()) {
            throw new BadRequestException("User not found by id: " + id);
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
                passwordEncoder.encode(randomUUID().toString())
                : passwordEncoder.encode(updatedUser.getPassword().trim()));

        userDao.update(
                updatedUser.getId(),
                updatedUser.getUsername().trim(),
                updatedUser.getName().trim(),
                password,
                updatedUser.isEnabled(),
                Instant.now(defaultClock)
        );

        return ResponseEntity.ok("{}");
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteAccounts(@RequestBody final DeleteAccountsRequest request) {
        val adminUser = getCurrentUserVerifyAdmin();
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
        return ResponseEntity.ok("{}");
    }
}
