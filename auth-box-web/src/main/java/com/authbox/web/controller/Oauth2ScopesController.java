package com.authbox.web.controller;

import com.authbox.web.config.Constants;
import com.authbox.web.model.CreateScopeRequest;
import com.authbox.web.model.DeleteScopesRequest;
import com.authbox.web.service.Oauth2ScopesService;
import com.authbox.web.service.Oauth2ScopesService.OauthScopeDto;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(Constants.API_PREFIX + "/oauth2-scope")
@AllArgsConstructor
public class Oauth2ScopesController extends BaseController {

    private final Oauth2ScopesService oauth2ScopesService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Page<OauthScopeDto> getOauth2Scopes(@RequestParam(value = "pageSize", defaultValue = "10") final int pageSize,
                                               @RequestParam(value = "currentPage", defaultValue = "0") final int currentPage) {
        return oauth2ScopesService.getOauth2Scopes(getOrganization(), pageSize, currentPage);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public OauthScopeDto getOauth2ScopeById(final String id) {
        return oauth2ScopesService.getOauth2ScopeById(getOrganization(), id);
    }

    @PostMapping("/count-clients")
    @PreAuthorize("isAuthenticated()")
    public long countClientsUsingScopeId(@RequestBody final DeleteScopesRequest request) {
        return oauth2ScopesService.countClientsUsingScopeId(getOrganization(), request);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public OauthScopeDto createScope(@RequestBody final CreateScopeRequest createScopeRequest) {
        return oauth2ScopesService.createScope(getOrganization(), createScopeRequest);
    }

    @PostMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public OauthScopeDto updateScope(@RequestBody final OauthScopeDto updatedOauthScope) {
        return oauth2ScopesService.updateScope(getOrganization(), updatedOauthScope);
    }

    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public void deleteScope(@RequestBody final DeleteScopesRequest deleteScopesRequest) {
        oauth2ScopesService.deleteScope(getOrganization(), deleteScopesRequest);
    }
}
