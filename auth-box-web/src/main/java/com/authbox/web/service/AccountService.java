package com.authbox.web.service;

import com.authbox.web.model.CreateAccountRequest;
import com.authbox.web.model.DeleteAccountsRequest;
import com.authbox.web.model.PasswordChangeRequest;
import com.authbox.web.model.UpdateUserRequest;
import com.authbox.web.model.UserDto;
import org.springframework.data.domain.Page;

public interface AccountService {

    /**
     * Updates user entity using UpdateUserRequest object
     */
    UserDto updateCurrentAccount(UserDto currentUser, UpdateUserRequest updatedUser);

    /**
     * Updates password for current user using PasswordChangeRequest object
     */
    UserDto updateCurrentAccountPassword(UserDto currentUser, PasswordChangeRequest passwordChangeRequest);

    /**
     * Returns paginated list of users (admin only)
     */
    Page<UserDto> listAccounts(UserDto adminUser, int pageSize, int currentPage);

    /**
     * Returns user by id (admin only)
     */
    UserDto getAccount(UserDto adminUser, String id);

    /**
     * Create new user (admin only)
     */
    UserDto createAccountForOrganization(UserDto adminUser, CreateAccountRequest request);

    /**
     * Updates user by id using UpdateUserRequest object (admin only)
     */
    void updateAccount(UserDto adminUser, String id, UpdateUserRequest updatedUser);

    /**
     * Deletes user using DeleteAccountsRequest.accountIds (admin only)
     */
    void deleteAccounts(UserDto adminUser, DeleteAccountsRequest request);
}
