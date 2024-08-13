package com.authbox.web.controller;

import com.authbox.web.model.CreateAccountWithOrganizationRequest;
import com.authbox.web.service.RegistrationService;
import com.authbox.web.service.RegistrationService.UserDto;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/registration")
@AllArgsConstructor
@Hidden // Disable OpenAPI spec for this controller
public class RegistrationController extends BaseController {

    private final RegistrationService registrationService;

    @PostMapping
    public UserDto createAccountWithOrganization(@RequestBody final CreateAccountWithOrganizationRequest request) {
        return registrationService.createAccountWithOrganization(request);
    }
}
