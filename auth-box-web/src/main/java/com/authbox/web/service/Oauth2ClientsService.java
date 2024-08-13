package com.authbox.web.service;

import com.authbox.base.exception.AccessDeniedException;
import com.authbox.base.exception.EntityNotFoundException;
import com.authbox.base.model.GrantType;
import com.authbox.base.model.OauthClient;
import com.authbox.base.model.OauthScope;
import com.authbox.base.model.Organization;
import com.authbox.base.model.TokenFormat;
import com.authbox.base.model.UpdateOauthClientRequest;
import com.authbox.web.model.DeleteClientsRequest;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public interface Oauth2ClientsService {

    /**
     * Verifies request criteria and returns OauthClientDto object based on id provided
     */
    OauthClientDto getOauth2ClientById(String clientId, Organization organization) throws EntityNotFoundException, AccessDeniedException;

    /**
     * Returns paginated list of OauthClientDto objects for provided organization
     */
    Page<OauthClientDto> getOauth2Clients(Organization organization, int currentPage, int pageSize);

    /**
     * Updates OAuth2 client specified by client id and organization with values in UpdateOauthClientRequest object
     * and returns updated OauthClientDto object
     */
    OauthClientDto updateOauth2ClientById(String clientId, Organization organization, UpdateOauthClientRequest updatedOauthClient);

    /**
     * Creates a new OAuth2 client for provided organization using specified update request object
     */
    OauthClientDto createOauth2Client(Organization organization, UpdateOauthClientRequest newOauthClient);

    /**
     * Deletes one or more clients for provided organization using DeleteClientsRequest object
     */
    void deleteClients(Organization organization, DeleteClientsRequest deleteClientsRequest);

    /**
     * Creates new keys for provided organization and client id
     */
    OauthClientDto createNewKeys(String clientId, Organization organization);

    /**
     * Assigns new keys for provided organization and specified client id
     */
    OauthClientDto assignKeys(String clientId, Organization organization, String publicKeyString, String privateKeyString);

    @Builder
    @Getter
    class OauthClientDto {
        private String id;
        private Instant createTime;
        private String description;
        private String secret;
        private List<GrantType> grantTypes;
        private String organizationId;
        private boolean enabled;
        private List<String> redirectUrls;
        private Duration expiration;
        private Duration refreshExpiration;
        private TokenFormat tokenFormat;
        private String publicKey;
        private Instant lastUpdated;
        private List<OauthScope> scopes;
        private List<String> scopeIds;

        static List<OauthClientDto> fromEntityCollection(Collection<OauthClient> entities) {
            return entities.stream()
                    .map(OauthClientDto::fromEntity)
                    .collect(Collectors.toList());
        }

        static OauthClientDto fromEntity(OauthClient entity) {
            return OauthClientDto.builder()
                    .id(entity.getId())
                    .createTime(entity.getCreateTime())
                    .description(entity.getDescription())
                    .secret(entity.getSecret())
                    .grantTypes(entity.getGrantTypes())
                    .organizationId(entity.getOrganizationId())
                    .enabled(entity.isEnabled())
                    .redirectUrls(entity.getRedirectUrls())
                    .expiration(entity.getExpiration())
                    .refreshExpiration(entity.getRefreshExpiration())
                    .tokenFormat(entity.getTokenFormat())
                    .publicKey(entity.getPublicKey())
                    .lastUpdated(entity.getLastUpdated())
                    .scopes(entity.getScopes())
                    .scopeIds(entity.getScopeIds())
                    .build();
        }
    }

}
