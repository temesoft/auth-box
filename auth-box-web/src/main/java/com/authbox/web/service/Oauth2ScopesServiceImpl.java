package com.authbox.web.service;

import com.authbox.base.dao.OauthClientScopeDao;
import com.authbox.base.dao.OauthScopeDao;
import com.authbox.base.exception.AccessDeniedException;
import com.authbox.base.exception.BadRequestException;
import com.authbox.base.exception.EntityNotFoundException;
import com.authbox.base.model.OauthScope;
import com.authbox.base.model.Organization;
import com.authbox.web.config.Constants;
import com.authbox.web.model.CreateScopeRequest;
import com.authbox.web.model.DeleteScopesRequest;
import lombok.AllArgsConstructor;
import lombok.val;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.function.Supplier;

import static com.authbox.base.util.IdUtils.createId;
import static org.springframework.util.ObjectUtils.isEmpty;

@Service
@AllArgsConstructor
public class Oauth2ScopesServiceImpl implements Oauth2ScopesService {

    private final Clock defaultClock;
    private final OauthScopeDao oauthScopeDao;
    private final OauthClientScopeDao oauthClientScopeDao;

    /**
     * Returns paginated list of OauthScopeDto objects for provided organization
     */
    @Override
    public Page<OauthScopeDto> getOauth2Scopes(final Organization organization, final int pageSize, final int currentPage) {
        return oauthScopeDao.listByOrganizationId(organization.getId(), PageRequest.of(currentPage, pageSize))
                .map(OauthScopeDto::fromEntity);
    }

    /**
     * Returns a OauthScopeDto for provided organization based on client id
     */
    @Override
    public OauthScopeDto getOauth2ScopeById(final Organization organization, final String id) {
        val oauthScope = oauthScopeDao.getById(id);
        if (oauthScope.isEmpty()) {
            throwScopeNotFound(id);
        }
        if (!organization.getId().equals(oauthScope.get().getOrganizationId())) {
            throw new AccessDeniedException();
        }
        return OauthScopeDto.fromEntity(oauthScope.get());
    }

    /**
     * Returns a count of clients using specified scope for provided organization
     */
    @Override
    public long countClientsUsingScopeId(final Organization organization, final DeleteScopesRequest request) {
        request.scopeIds.stream().parallel().forEach(scopeId -> {
            val oauthScope = oauthScopeDao.getById(scopeId);
            if (oauthScope.isEmpty()) {
                throwScopeNotFound(scopeId);
            }
            if (!organization.getId().equals(oauthScope.get().getOrganizationId())) {
                throw new AccessDeniedException();
            }
        });
        return oauthClientScopeDao.countByScopeIds(request.scopeIds);
    }

    /**
     * Creates scope for provided organization using CreateScopeRequest
     */
    @Override
    public OauthScopeDto createScope(final Organization organization, final CreateScopeRequest createScopeRequest) {
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
        val id = createId();
        if (oauthScopeDao.existsByOrganizationIdAndScope(organization.getId().trim(), createScopeRequest.scope.trim())) {
            throw new BadRequestException("Scope '" + createScopeRequest.scope.trim() + "' already exists");
        }
        val result = new OauthScope(
                id,
                Instant.now(defaultClock),
                createScopeRequest.description.trim(),
                createScopeRequest.scope.trim(),
                organization.getId()
        );
        oauthScopeDao.insert(result);
        return OauthScopeDto.fromEntity(result);
    }

    /**
     * Updates scope for provided organization using specified OauthScopeDto
     */
    @Override
    public OauthScopeDto updateScope(final Organization organization, final OauthScopeDto updatedOauthScope) {
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
        val existingOauthScope = oauthScopeDao.getById(updatedOauthScope.getId());
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
        return OauthScopeDto.fromEntity(
                oauthScopeDao.getById(updatedOauthScope.getId()).orElseThrow(scopeNotFound(updatedOauthScope.getId()))
        );
    }

    /**
     * Deletes scopes for provided organization using DeleteScopesRequest.scopeIds
     */
    @Override
    public void deleteScope(final Organization organization, final DeleteScopesRequest deleteScopesRequest) {
        if (isEmpty(deleteScopesRequest.scopeIds)) {
            throw new BadRequestException("Scope can not be empty");
        }
        deleteScopesRequest.scopeIds
                .forEach(scopeId -> {
                    val oauthScope = oauthScopeDao.getById(scopeId);
                    if (oauthScope.isEmpty()) {
                        throwScopeNotFound(scopeId);
                    }
                    if (!organization.getId().equals(oauthScope.get().getOrganizationId())) {
                        throw new AccessDeniedException();
                    }
                    oauthClientScopeDao.deleteByScopeId(scopeId);
                    oauthScopeDao.deleteById(scopeId);
                });
    }

    private void throwScopeNotFound(final String scopeId) {
        throw new EntityNotFoundException("Scope not found by id: " + scopeId);
    }

    private Supplier<EntityNotFoundException> scopeNotFound(final String scopeId) {
        return () -> new EntityNotFoundException("Scope not found by id: " + scopeId);
    }
}
