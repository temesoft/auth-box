package com.authbox.web.controller;

import com.authbox.base.model.UpdateOauthUserRequest;
import com.authbox.web.model.DeleteUsersRequest;
import com.authbox.web.model.PasswordChangeRequest;
import com.authbox.web.service.Oauth2UsersService;
import com.authbox.web.service.Oauth2UsersService.OauthUserDto;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.awt.image.BufferedImage;

import static com.authbox.web.config.Constants.API_PREFIX;
import static org.springframework.http.MediaType.IMAGE_PNG_VALUE;

@RestController
@AllArgsConstructor
@RequestMapping(API_PREFIX + "/oauth2-user")
public class Oauth2UsersController extends BaseController {

    private final Oauth2UsersService oauth2UsersService;

    @GetMapping
    public Page<OauthUserDto> getOauth2Users(@RequestParam(value = "pageSize", defaultValue = "10") final int pageSize,
                                             @RequestParam(value = "currentPage", defaultValue = "0") final int currentPage) {
        return oauth2UsersService.getOauth2Users(getOrganization(), pageSize, currentPage);
    }

    @GetMapping("/{id}")
    public OauthUserDto getOauth2UserById(@PathVariable("id") final String id) {
        return oauth2UsersService.getOauth2UserById(getOrganization(), id);
    }

    @PostMapping("/{id}/password-reset")
    public OauthUserDto updatePassword(@PathVariable("id") final String userId,
                                       @RequestBody final PasswordChangeRequest passwordChangeRequest) {
        return oauth2UsersService.updatePassword(getOrganization(), userId, passwordChangeRequest);
    }

    @GetMapping(value = "/{id}/2fa-qr-code", produces = IMAGE_PNG_VALUE)
    public BufferedImage generate2FaQrCodeImage(@PathVariable("id") final String userId) {
        return oauth2UsersService.generate2FaQrCodeImage(getOrganization(), userId);
    }

    @PostMapping("/{id}")
    @Transactional
    public OauthUserDto updateOauth2UserById(@PathVariable("id") final String userId,
                                             @RequestBody final UpdateOauthUserRequest updatedOauthUser) {
        return oauth2UsersService.updateOauth2UserById(getOrganization(), userId, updatedOauthUser);
    }

    @PostMapping
    public OauthUserDto createOauth2User(@RequestBody final UpdateOauthUserRequest newOauthUser) {
        return oauth2UsersService.createOauth2User(getOrganization(), newOauthUser);
    }

    @DeleteMapping
    public void deleteUsers(@RequestBody final DeleteUsersRequest deleteUsersRequest) {
        oauth2UsersService.deleteUsers(getOrganization(), deleteUsersRequest);
    }
}