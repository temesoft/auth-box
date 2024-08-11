package com.authbox.web.controller;

import com.authbox.base.exception.AccessDeniedException;
import com.authbox.base.exception.BadRequestException;
import com.authbox.base.exception.EntityNotFoundException;
import com.authbox.base.model.OauthScope;
import com.authbox.base.model.Organization;
import com.authbox.web.config.Constants;
import com.authbox.web.model.CreateScopeRequest;
import com.authbox.web.model.DeleteScopesRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.util.ObjectUtils.isEmpty;

@RestController
@RequestMapping(Constants.API_PREFIX + "/oauth2-scope")
public class Oauth2ScopesController extends BaseController {

    private final Clock defaultClock;

    public Oauth2ScopesController(final Clock defaultClock) {
        this.defaultClock = defaultClock;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Page<OauthScope> getOauth2Scopes(@RequestParam(value = "pageSize", defaultValue = "10") final int pageSize,
                                            @RequestParam(value = "currentPage", defaultValue = "0") final int currentPage) {
        final Organization organization = getOrganization();
        return oauthScopeDao.listByOrganizationId(organization.getId(), PageRequest.of(currentPage, pageSize));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public OauthScope getOauth2ScopeById(final String id) {
        final Organization organization = getOrganization();

        final Optional<OauthScope> oauthScope = oauthScopeDao.getById(id);
        if (oauthScope.isEmpty()) {
            throw new EntityNotFoundException("Scope not found by id: " + id);
        }

        if (!organization.getId().equals(oauthScope.get().getOrganizationId())) {
            throw new AccessDeniedException();
        }
        return oauthScope.get();
    }

    @PostMapping("/count-clients")
    @PreAuthorize("isAuthenticated()")
    public long countClientsUsingScopeId(@RequestBody final DeleteScopesRequest request) {
        final Organization organization = getOrganization();

        request.scopeIds.stream().parallel().forEach(scopeId -> {
            final Optional<OauthScope> oauthScope = oauthScopeDao.getById(scopeId);
            if (oauthScope.isEmpty()) {
                throw new EntityNotFoundException("Scope not found by id: " + scopeId);
            }

            if (!organization.getId().equals(oauthScope.get().getOrganizationId())) {
                throw new AccessDeniedException();
            }
        });

        return oauthClientScopeDao.countByScopeIds(request.scopeIds);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public OauthScope createScope(@RequestBody final CreateScopeRequest createScopeRequest) {
        final Organization organization = getOrganization();

        if (isEmpty(createScopeRequest.scope)) {
            throw new BadRequestException("Scope can not be empty");
        }

        if (createScopeRequest.scope.trim().contains(Constants.SPACE)) {
            throw new BadRequestException("Scope can not have spaces");
        }

        if (createScopeRequest.scope.trim().contains(Constants.COMMA)) {
            throw new BadRequestException("Scope can not have commas");
        }

        if (isEmpty(createScopeRequest.description)) {
            throw new BadRequestException("Description can not be empty");
        }

        final String id = UUID.randomUUID().toString();

        if (oauthScopeDao.existsByOrganizationIdAndScope(organization.getId().trim(), createScopeRequest.scope.trim())) {
            throw new BadRequestException("Scope '" + createScopeRequest.scope.trim() + "' already exists");
        }

        final OauthScope result = new OauthScope(
                id,
                Instant.now(defaultClock),
                createScopeRequest.description.trim(),
                createScopeRequest.scope.trim(),
                organization.getId()
        );
        oauthScopeDao.insert(result);
        return result;
    }

    @PostMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public OauthScope updateScope(@RequestBody final OauthScope updatedOauthScope) {
        final Organization organization = getOrganization();

        if (isEmpty(updatedOauthScope.getScope())) {
            throw new BadRequestException("Scope can not be empty");
        }

        if (updatedOauthScope.getScope().trim().contains(Constants.SPACE)) {
            throw new BadRequestException("Scope can not have spaces");
        }

        if (updatedOauthScope.getScope().trim().contains(Constants.COMMA)) {
            throw new BadRequestException("Scope can not have commas");
        }

        if (isEmpty(updatedOauthScope.getDescription())) {
            throw new BadRequestException("Description can not be empty");
        }

        final Optional<OauthScope> existingOauthScope = oauthScopeDao.getById(updatedOauthScope.getId());
        if (existingOauthScope.isEmpty()) {
            throw new EntityNotFoundException("Scope not found by id: " + updatedOauthScope.getId());
        }

        // check if scope name changed, verify is scope with same name does not exist already
        if (!updatedOauthScope.getScope().equals(existingOauthScope.get().getScope())) {
            if (oauthScopeDao.existsByOrganizationIdAndScope(organization.getId().trim(), updatedOauthScope.getScope().trim())) {
                throw new BadRequestException("Scope '" + updatedOauthScope.getScope().trim() + "' already exists");
            }
        }
        oauthScopeDao.update(updatedOauthScope.getId(), updatedOauthScope.getScope().trim(), updatedOauthScope.getDescription().trim());
        return oauthScopeDao.getById(updatedOauthScope.getId()).orElseThrow(() -> new EntityNotFoundException("Scope not found by id: " + updatedOauthScope.getId()));
    }

    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public void deleteScope(@RequestBody final DeleteScopesRequest deleteScopesRequest) {
        final Organization organization = getOrganization();

        if (isEmpty(deleteScopesRequest.scopeIds)) {
            throw new BadRequestException("Scope can not be empty");
        }

        deleteScopesRequest.scopeIds
                .forEach(scopeId -> {
                    final Optional<OauthScope> oauthScope = oauthScopeDao.getById(scopeId);
                    if (oauthScope.isEmpty()) {
                        throw new EntityNotFoundException("Scope not found by id: " + scopeId);
                    }

                    if (!organization.getId().equals(oauthScope.get().getOrganizationId())) {
                        throw new AccessDeniedException();
                    }

                    oauthClientScopeDao.deleteByScopeId(scopeId);
                    oauthScopeDao.deleteById(scopeId);
                });
    }
}
