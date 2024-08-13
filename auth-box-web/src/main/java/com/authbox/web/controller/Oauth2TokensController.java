package com.authbox.web.controller;

import com.authbox.web.config.Constants;
import com.authbox.web.model.DeleteTokensRequest;
import com.authbox.web.service.Oauth2TokensService;
import com.authbox.web.service.Oauth2TokensService.OauthTokenDto;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(Constants.API_PREFIX + "/oauth2-token")
@AllArgsConstructor
public class Oauth2TokensController extends BaseController {

    private final Oauth2TokensService oauth2TokensService;

    @GetMapping("/client/{clientId}")
    @PreAuthorize("isAuthenticated()")
    public Page<OauthTokenDto> getOauth2TokensByClientId(@PathVariable("clientId") final String clientId,
                                                         @RequestParam(value = "pageSize", defaultValue = "10") final int pageSize,
                                                         @RequestParam(value = "currentPage", defaultValue = "0") final int currentPage) {
        return oauth2TokensService.getOauth2TokensByClientId(getOrganization(), clientId, pageSize, currentPage);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public Page<OauthTokenDto> getOauth2TokensByUserId(@PathVariable("userId") final String userId,
                                                       @RequestParam(value = "pageSize", defaultValue = "10") final int pageSize,
                                                       @RequestParam(value = "currentPage", defaultValue = "0") final int currentPage) {
        return oauth2TokensService.getOauth2TokensByUserId(getOrganization(), userId, pageSize, currentPage);
    }

    @GetMapping("/list")
    @PreAuthorize("isAuthenticated()")
    public Page<OauthTokenDto> listOauth2Token(@RequestParam(value = "pageSize", defaultValue = "10") final int pageSize,
                                               @RequestParam(value = "currentPage", defaultValue = "0") final int currentPage) {
        return oauth2TokensService.listOauth2Token(getOrganization(), pageSize, currentPage);
    }

    @GetMapping("/hash/{hash}")
    @PreAuthorize("isAuthenticated()")
    public OauthTokenDto getOauth2TokenByHash(@PathVariable("hash") final String hash) {
        return oauth2TokensService.getOauth2TokenByHash(getOrganization(), hash);
    }

    @GetMapping("/token/{token}")
    @PreAuthorize("isAuthenticated()")
    public OauthTokenDto getOauth2TokenByToken(@PathVariable("token") final String token) {
        return oauth2TokensService.getOauth2TokenByToken(getOrganization(), token);
    }

    @GetMapping("/id/{id}")
    @PreAuthorize("isAuthenticated()")
    public OauthTokenDto getOauth2TokenById(@PathVariable("id") final String id) {
        return oauth2TokensService.getOauth2TokenById(getOrganization(), id);
    }

    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    public void deleteOauth2Tokens(@RequestBody final DeleteTokensRequest deleteTokensRequest) {
        oauth2TokensService.deleteOauth2Tokens(getOrganization(), deleteTokensRequest);
    }
}