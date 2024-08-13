package com.authbox.web.controller;

import com.authbox.base.model.UpdateOauthClientRequest;
import com.authbox.web.config.Constants;
import com.authbox.web.model.DeleteClientsRequest;
import com.authbox.web.service.Oauth2ClientsService;
import com.authbox.web.service.Oauth2ClientsService.OauthClientDto;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
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
@RequestMapping(Constants.API_PREFIX + "/oauth2-client")
@AllArgsConstructor
public class Oauth2ClientsController extends BaseController {

    private final Oauth2ClientsService oauth2ClientsService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Page<OauthClientDto> getOauth2Clients(
            @RequestParam(value = "pageSize", defaultValue = "10") final int pageSize,
            @RequestParam(value = "currentPage", defaultValue = "0") final int currentPage) {
        return oauth2ClientsService.getOauth2Clients(getOrganization(), currentPage, pageSize);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public OauthClientDto getOauth2ClientById(@PathVariable("id") final String id) {
        return oauth2ClientsService.getOauth2ClientById(id, getOrganization());
    }

    @PostMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public OauthClientDto updateOauth2ClientById(
            @PathVariable("id") final String clientId,
            @RequestBody final UpdateOauthClientRequest updatedOauthClient) {
        return oauth2ClientsService.updateOauth2ClientById(clientId, getOrganization(), updatedOauthClient);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public OauthClientDto createOauth2Client(@RequestBody final UpdateOauthClientRequest newOauthClient) {
        return oauth2ClientsService.createOauth2Client(getOrganization(), newOauthClient);
    }

    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    public void deleteClients(@RequestBody final DeleteClientsRequest deleteClientsRequest) {
        oauth2ClientsService.deleteClients(getOrganization(), deleteClientsRequest);
    }

    @PostMapping("/{id}/create-new-keys")
    @PreAuthorize("hasAuthority('SCOPE_organization/write') OR isAuthenticated()")
    public OauthClientDto createNewKeys(@PathVariable("id") final String clientId) {
        return oauth2ClientsService.createNewKeys(clientId, getOrganization());
    }

    @PostMapping(value = "/{id}/assign-keys", consumes = "application/x-www-form-urlencoded")
    @PreAuthorize("hasAuthority('SCOPE_organization/write') OR isAuthenticated()")
    public OauthClientDto assignKeys(
            @PathVariable("id") final String clientId,
            @RequestParam("publicKey") final String publicKeyString,
            @RequestParam("privateKey") final String privateKeyString) {
        return oauth2ClientsService.assignKeys(clientId, getOrganization(), publicKeyString, privateKeyString);
    }
}
