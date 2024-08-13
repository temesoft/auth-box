package com.authbox.web.service;

import com.authbox.base.model.OauthScope;
import com.authbox.base.model.Organization;
import com.authbox.web.model.CreateScopeRequest;
import com.authbox.web.model.DeleteScopesRequest;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.time.Instant;

public interface Oauth2ScopesService {

    /**
     * Returns paginated list of OauthScopeDto objects for provided organization
     */
    Page<OauthScopeDto> getOauth2Scopes(Organization organization, int pageSize, int currentPage);

    /**
     * Returns a OauthScopeDto for provided organization based on client id
     */
    OauthScopeDto getOauth2ScopeById(Organization organization, String id);

    /**
     * Returns a count of clients using specified scope for provided organization
     */
    long countClientsUsingScopeId(Organization organization, DeleteScopesRequest request);

    /**
     * Creates scope for provided organization using CreateScopeRequest
     */
    OauthScopeDto createScope(Organization organization, CreateScopeRequest createScopeRequest);

    /**
     * Updates scope for provided organization using specified OauthScopeDto
     */
    OauthScopeDto updateScope(Organization organization, OauthScopeDto updatedOauthScope);

    /**
     * Deletes scopes for provided organization using DeleteScopesRequest.scopeIds
     */
    void deleteScope(Organization organization, DeleteScopesRequest deleteScopesRequest);

    @Builder
    @Getter
    class OauthScopeDto {
        private String id;
        private Instant createTime;
        private String description;
        private String scope;
        private String organizationId;

        static OauthScopeDto fromEntity(OauthScope entity) {
            return OauthScopeDto.builder()
                    .id(entity.getId())
                    .createTime(entity.getCreateTime())
                    .description(entity.getDescription())
                    .scope(entity.getScope())
                    .organizationId(entity.getOrganizationId())
                    .build();
        }
    }
}
