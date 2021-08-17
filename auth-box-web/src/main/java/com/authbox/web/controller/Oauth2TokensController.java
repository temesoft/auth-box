package com.authbox.web.controller;

import com.authbox.base.exception.AccessDeniedException;
import com.authbox.base.exception.EntityNotFoundException;
import com.authbox.base.model.OauthClient;
import com.authbox.base.model.OauthToken;
import com.authbox.base.model.OauthUser;
import com.authbox.base.model.Organization;
import com.authbox.web.config.Constants;
import com.authbox.web.model.DeleteTokensRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.util.Optional;

import static com.authbox.base.util.HashUtils.sha256;

@RestController
@RequestMapping(Constants.API_PREFIX + "/oauth2-token")
public class Oauth2TokensController extends BaseController {

    private final Clock defaultClock;

    public Oauth2TokensController(final Clock defaultClock) {
        this.defaultClock = defaultClock;
    }

    @GetMapping("/client/{clientId}")
    @PreAuthorize("isAuthenticated()")
    public Page<OauthToken> getOauth2TokensByClientId(@PathVariable("clientId") final String clientId,
                                                      @RequestParam(value = "pageSize", defaultValue = "10") final int pageSize,
                                                      @RequestParam(value = "currentPage", defaultValue = "0") final int currentPage) {
        final Organization organization = getOrganization();
        final Optional<OauthClient> oauthClient = oauthClientDao.getById(clientId);
        if (oauthClient.isEmpty()) {
            throw new EntityNotFoundException("Oauth client not found by id: " + clientId);
        }
        if (!organization.id.equals(oauthClient.get().organizationId)) {
            throw new AccessDeniedException();
        }
        return oauthTokenDao.listByClientId(clientId, PageRequest.of(currentPage, pageSize));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public Page<OauthToken> getOauth2TokensByUserId(@PathVariable("userId") final String userId,
                                                    @RequestParam(value = "pageSize", defaultValue = "10") final int pageSize,
                                                    @RequestParam(value = "currentPage", defaultValue = "0") final int currentPage) {
        final Organization organization = getOrganization();
        final Optional<OauthUser> oauthUser = oauthUserDao.getById(userId);
        if (oauthUser.isEmpty()) {
            throw new EntityNotFoundException("Oauth user not found by id: " + userId);
        }
        if (!organization.id.equals(oauthUser.get().organizationId)) {
            throw new AccessDeniedException();
        }
        return oauthTokenDao.listByUserId(userId, PageRequest.of(currentPage, pageSize));
    }

    @GetMapping("/list")
    @PreAuthorize("isAuthenticated()")
    public Page<OauthToken> listOauth2Token(@RequestParam(value = "pageSize", defaultValue = "10") final int pageSize,
                                            @RequestParam(value = "currentPage", defaultValue = "0") final int currentPage) {
        final Organization organization = getOrganization();
        return oauthTokenDao.listByOrganizationId(organization.id, PageRequest.of(currentPage, pageSize));
    }

    @GetMapping("/hash/{hash}")
    @PreAuthorize("isAuthenticated()")
    public OauthToken getOauth2TokenByHash(@PathVariable("hash") final String hash) {
        final Organization organization = getOrganization();
        final Optional<OauthToken> oauthToken = oauthTokenDao.getByHash(hash);
        if (oauthToken.isEmpty()) {
            throw new EntityNotFoundException("Oauth token not found by hash: " + hash);
        }
        if (!organization.id.equals(oauthToken.get().organizationId)) {
            throw new AccessDeniedException();
        }
        return oauthToken.get();
    }

    @GetMapping("/token/{token}")
    @PreAuthorize("isAuthenticated()")
    public OauthToken getOauth2TokenByToken(@PathVariable("token") final String token) {
        final Organization organization = getOrganization();
        final String hash = sha256(token);
        final Optional<OauthToken> oauthToken = oauthTokenDao.getByHash(hash);
        if (oauthToken.isEmpty()) {
            throw new EntityNotFoundException("Oauth token not found by token value: " + token);
        }
        if (!organization.id.equals(oauthToken.get().organizationId)) {
            throw new AccessDeniedException();
        }
        return oauthToken.get();
    }

    @GetMapping("/id/{id}")
    @PreAuthorize("isAuthenticated()")
    public OauthToken getOauth2TokenById(@PathVariable("id") final String id) {
        final Organization organization = getOrganization();
        final Optional<OauthToken> oauthToken = oauthTokenDao.getById(id);
        if (oauthToken.isEmpty()) {
            throw new EntityNotFoundException("Oauth token not found by id: " + id);
        }
        if (!organization.id.equals(oauthToken.get().organizationId)) {
            throw new AccessDeniedException();
        }
        return oauthToken.get();
    }

    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    public void deleteOauth2Tokens(@RequestBody final DeleteTokensRequest deleteTokensRequest) {
        final Organization organization = getOrganization();
        deleteTokensRequest.tokenIds.stream().parallel().forEach(id -> {
            final Optional<OauthToken> oauthToken = oauthTokenDao.getById(id);
            if (oauthToken.isEmpty()) {
                throw new EntityNotFoundException("Oauth token not found by id: " + id);
            }
            if (!organization.id.equals(oauthToken.get().organizationId)) {
                throw new AccessDeniedException();
            }
            oauthTokenDao.deleteById(oauthToken.get().id, oauthToken.get().hash);
        });
    }
}