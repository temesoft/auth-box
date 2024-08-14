package com.authbox.web.service;

import com.authbox.web.model.CreateAccountWithOrganizationRequest;
import com.authbox.web.model.UserDto;

public interface RegistrationService {

    /**
     * Creates a new account with new organization using CreateAccountWithOrganizationRequest object
     */
    UserDto createAccountWithOrganization(CreateAccountWithOrganizationRequest request);
}
