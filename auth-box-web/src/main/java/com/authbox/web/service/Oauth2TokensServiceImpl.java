package com.authbox.web.service;

import com.authbox.base.dao.OauthClientDao;
import com.authbox.base.dao.OauthTokenDao;
import com.authbox.base.dao.OauthUserDao;
import com.authbox.base.exception.AccessDeniedException;
import com.authbox.base.exception.EntityNotFoundException;
import com.authbox.base.model.OauthToken;
import com.authbox.base.model.Organization;
import com.authbox.web.model.DeleteTokensRequest;
import lombok.AllArgsConstructor;
import lombok.val;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static com.authbox.base.util.HashUtils.sha256;

@Service
@AllArgsConstructor
public class Oauth2TokensServiceImpl implements Oauth2TokensService {

    private final OauthClientDao oauthClientDao;
    private final OauthTokenDao oauthTokenDao;
    private final OauthUserDao oauthUserDao;

    /**
     * Returns paginated list of OauthTokenDto objects for provided organization by client id
     */
    @Override
    public Page<OauthTokenDto> getOauth2TokensByClientId(final Organization organization, final String clientId, final int pageSize, final int currentPage) {
        val oauthClient = oauthClientDao.getById(clientId);
        if (oauthClient.isEmpty()) {
            throw new EntityNotFoundException("Oauth client not found by id: " + clientId);
        }
        if (!organization.getId().equals(oauthClient.get().getOrganizationId())) {
            throw new AccessDeniedException();
        }
        return oauthTokenDao.listByClientId(clientId, PageRequest.of(currentPage, pageSize))
                .map(OauthTokenDto::fromEntity);
    }

    /**
     * Returns paginated list of OauthTokenDto objects for provided organization by user id
     */
    @Override
    public Page<OauthTokenDto> getOauth2TokensByUserId(final Organization organization, final String userId, final int pageSize, final int currentPage) {
        val oauthUser = oauthUserDao.getById(userId);
        if (oauthUser.isEmpty()) {
            throw new EntityNotFoundException("Oauth user not found by id: " + userId);
        }
        if (!organization.getId().equals(oauthUser.get().getOrganizationId())) {
            throw new AccessDeniedException();
        }
        return oauthTokenDao.listByUserId(userId, PageRequest.of(currentPage, pageSize))
                .map(OauthTokenDto::fromEntity);
    }

    /**
     * Returns paginated list of OauthTokenDto objects for provided organization
     */
    @Override
    public Page<OauthTokenDto> listOauth2Token(final Organization organization, final int pageSize, final int currentPage) {
        return oauthTokenDao.listByOrganizationId(organization.getId(), PageRequest.of(currentPage, pageSize))
                .map(OauthTokenDto::fromEntity);
    }

    /**
     * Returns OauthTokenDto object for provided organization by hash
     */
    @Override
    public OauthTokenDto getOauth2TokenByHash(final Organization organization, final String hash) {
        val oauthToken = oauthTokenDao.getByHash(hash);
        if (oauthToken.isEmpty()) {
            throw new EntityNotFoundException("Oauth token not found by hash: " + hash);
        }
        if (!organization.getId().equals(oauthToken.get().getOrganizationId())) {
            throw new AccessDeniedException();
        }
        return OauthTokenDto.fromEntity(oauthToken.get());
    }

    /**
     * Returns OauthTokenDto object for provided organization by token value
     */
    @Override
    public OauthTokenDto getOauth2TokenByToken(final Organization organization, final String token) {
        val hash = sha256(token);
        val oauthToken = oauthTokenDao.getByHash(hash);
        if (oauthToken.isEmpty()) {
            throw new EntityNotFoundException("Oauth token not found by token value: " + token);
        }
        if (!organization.getId().equals(oauthToken.get().getOrganizationId())) {
            throw new AccessDeniedException();
        }
        return OauthTokenDto.fromEntity(oauthToken.get());
    }

    /**
     * Returns OauthTokenDto object for provided organization by id
     */
    @Override
    public OauthTokenDto getOauth2TokenById(final Organization organization, final String id) {
        val oauthToken = oauthTokenDao.getById(id);
        if (oauthToken.isEmpty()) {
            throw new EntityNotFoundException("Oauth token not found by id: " + id);
        }
        if (!organization.getId().equals(oauthToken.get().getOrganizationId())) {
            throw new AccessDeniedException();
        }
        return OauthTokenDto.fromEntity(oauthToken.get());
    }

    /**
     * Deletes OauthToken object for provided organization by DeleteTokensRequest.tokenIds
     */
    @Override
    public void deleteOauth2Tokens(final Organization organization, final DeleteTokensRequest deleteTokensRequest) {
        deleteTokensRequest.tokenIds.stream().parallel().forEach(id -> {
            final Optional<OauthToken> oauthToken = oauthTokenDao.getById(id);
            if (oauthToken.isEmpty()) {
                throw new EntityNotFoundException("Oauth token not found by id: " + id);
            }
            if (!organization.getId().equals(oauthToken.get().getOrganizationId())) {
                throw new AccessDeniedException();
            }
            oauthTokenDao.deleteById(oauthToken.get().getId(), oauthToken.get().getHash());
        });
    }
}
