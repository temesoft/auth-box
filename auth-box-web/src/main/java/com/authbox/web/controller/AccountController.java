package com.authbox.web.controller;

import com.authbox.web.config.Constants;
import com.authbox.web.model.CreateAccountRequest;
import com.authbox.web.model.DeleteAccountsRequest;
import com.authbox.web.model.PasswordChangeRequest;
import com.authbox.web.model.UpdateUserRequest;
import com.authbox.web.model.UserDto;
import com.authbox.web.service.AccountService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.val;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(Constants.API_PREFIX + "/account")
@AllArgsConstructor
public class AccountController extends BaseController {

    private final AccountService accountService;

    @GetMapping
    @PreAuthorize("hasRole('USER') || hasRole('ADMIN')")
    public UserDto getCurrentAccountDetails() {
        return getCurrentUser();
    }

    @PostMapping
    @PreAuthorize("hasRole('USER') || hasRole('ADMIN')")
    @Transactional
    public UserDto updateCurrentAccount(@RequestBody final UpdateUserRequest updatedUser) {
        return accountService.updateCurrentAccount(getCurrentUser(), updatedUser);
    }

    @PostMapping("/password")
    @PreAuthorize("hasRole('USER') || hasRole('ADMIN')")
    @Transactional
    public UserDto updateCurrentAccountPassword(@RequestBody final PasswordChangeRequest passwordChangeRequest) {
        return accountService.updateCurrentAccountPassword(getCurrentUser(), passwordChangeRequest);
    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<UserDto> listAccounts(@RequestParam(value = "pageSize", defaultValue = "10") final int pageSize,
                                      @RequestParam(value = "currentPage", defaultValue = "0") final int currentPage) {
        return accountService.listAccounts(getCurrentUserVerifyAdmin(), pageSize, currentPage);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto getAccount(@PathVariable("id") final String id) {
        val adminUser = getCurrentUserVerifyAdmin();
        return accountService.getAccount(getCurrentUserVerifyAdmin(), id);
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto createAccountForOrganization(@RequestBody final CreateAccountRequest request) {
        return accountService.createAccountForOrganization(getCurrentUserVerifyAdmin(), request);
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<String> updateAccount(@PathVariable("id") final String id,
                                                @RequestBody final UpdateUserRequest updatedUser) {
        accountService.updateAccount(getCurrentUserVerifyAdmin(), id, updatedUser);
        return ResponseEntity.ok("{}");
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteAccounts(@RequestBody final DeleteAccountsRequest request) {
        accountService.deleteAccounts(getCurrentUserVerifyAdmin(), request);
        return ResponseEntity.ok("{}");
    }
}
