package com.authbox.web.service;

import com.authbox.base.model.OauthToken;
import com.authbox.base.model.Organization;
import com.authbox.base.model.TokenType;
import com.authbox.web.model.DeleteTokensRequest;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;

public interface Oauth2TokensService {

    /**
     * Returns paginated list of OauthTokenDto objects for provided organization by client id
     */
    Page<OauthTokenDto> getOauth2TokensByClientId(Organization organization, String clientId, int pageSize, int currentPage);

    /**
     * Returns paginated list of OauthTokenDto objects for provided organization by user id
     */
    Page<OauthTokenDto> getOauth2TokensByUserId(Organization organization, String userId, int pageSize, int currentPage);

    /**
     * Returns paginated list of OauthTokenDto objects for provided organization
     */
    Page<OauthTokenDto> listOauth2Token(Organization organization, int pageSize, int currentPage);

    /**
     * Returns OauthTokenDto object for provided organization by hash
     */
    OauthTokenDto getOauth2TokenByHash(Organization organization, String hash);

    /**
     * Returns OauthTokenDto object for provided organization by token value
     */
    OauthTokenDto getOauth2TokenByToken(Organization organization, String token);

    /**
     * Returns OauthTokenDto object for provided organization by id
     */
    OauthTokenDto getOauth2TokenById(Organization organization, String id);

    /**
     * Deletes OauthToken object for provided organization by DeleteTokensRequest.tokenIds
     */
    void deleteOauth2Tokens(Organization organization, DeleteTokensRequest deleteTokensRequest);

    @Builder
    @Getter
    class OauthTokenDto {
        private String id;
        private Instant createTime;
        private String hash;
        private String organizationId;
        private String clientId;
        private Instant expiration;
        private List<String> scopes;
        private String oauthUserId;
        private TokenType tokenType;
        private String ip;
        private String userAgent;
        private String requestId;
        private String linkedTokenId;

        static OauthTokenDto fromEntity(OauthToken entity) {
            return OauthTokenDto.builder()
                    .id(entity.getId())
                    .createTime(entity.getCreateTime())
                    .hash(entity.getHash())
                    .organizationId(entity.getOrganizationId())
                    .clientId(entity.getClientId())
                    .expiration(entity.getExpiration())
                    .scopes(entity.getScopes())
                    .oauthUserId(entity.getOauthUserId())
                    .tokenType(entity.getTokenType())
                    .ip(entity.getIp())
                    .userAgent(entity.getUserAgent())
                    .requestId(entity.getRequestId())
                    .linkedTokenId(entity.getLinkedTokenId())
                    .build();
        }
    }

}
