package com.authbox.web.service;

import com.authbox.base.dao.OauthClientDao;
import com.authbox.base.dao.OauthClientScopeDao;
import com.authbox.base.exception.AccessDeniedException;
import com.authbox.base.exception.BadRequestException;
import com.authbox.base.exception.EntityNotFoundException;
import com.authbox.base.model.OauthClient;
import com.authbox.base.model.OauthClientScope;
import com.authbox.base.model.OauthScope;
import com.authbox.base.model.Organization;
import com.authbox.base.model.TokenFormat;
import com.authbox.base.model.UpdateOauthClientRequest;
import com.authbox.base.util.CertificateKeysUtils;
import com.authbox.web.model.DeleteClientsRequest;
import lombok.AllArgsConstructor;
import lombok.val;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.time.Clock;
import java.time.Instant;
import java.util.function.Supplier;

import static com.authbox.base.model.GrantType.authorization_code;
import static com.authbox.base.model.GrantType.refresh_token;
import static com.authbox.base.util.CertificateKeysUtils.generatePrivateKey;
import static com.authbox.base.util.CertificateKeysUtils.generatePublicKey;
import static com.authbox.base.util.HashUtils.sha256;
import static com.authbox.base.util.IdUtils.createId;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.util.ObjectUtils.isEmpty;

@Service
@AllArgsConstructor
public class Oauth2ClientsServiceImpl implements Oauth2ClientsService {

    private final Clock defaultClock;
    private final OauthClientDao oauthClientDao;
    private final OauthClientScopeDao oauthClientScopeDao;

    /**
     * Verifies request criteria and returns OauthClientDto object based on id provided
     */
    @Override
    public OauthClientDto getOauth2ClientById(final String clientId, final Organization organization) throws EntityNotFoundException, AccessDeniedException {
        val oauthClient = oauthClientDao.getById(clientId);
        if (oauthClient.isEmpty()) {
            throwClientNotFound(clientId);
        }
        if (!organization.getId().equals(oauthClient.get().getOrganizationId())) {
            throw new AccessDeniedException();
        }
        return OauthClientDto.fromEntity(oauthClient.get());
    }

    /**
     * Returns paginated list of OauthClientDto objects for provided organization
     */
    @Override
    public Page<OauthClientDto> getOauth2Clients(final Organization organization, final int currentPage, final int pageSize) {
        return oauthClientDao.listByOrganizationId(organization.getId(), PageRequest.of(currentPage, pageSize))
                .map(OauthClientDto::fromEntity);
    }

    /**
     * Updates OAuth2 client specified by client id and organization id with values in UpdateOauthClientRequest object
     * and returns updated OauthClientDto object
     */
    @Override
    public OauthClientDto updateOauth2ClientById(final String clientId,
                                                 final Organization organization,
                                                 final UpdateOauthClientRequest updatedOauthClient) {
        val oauthClient = oauthClientDao.getById(clientId);
        if (oauthClient.isEmpty()) {
            throwClientNotFound(clientId);
        }

        if (!organization.getId().equals(oauthClient.get().getOrganizationId())) {
            throw new AccessDeniedException();
        }

        val now = Instant.now(defaultClock);
        if (isNotBlank(updatedOauthClient.getPrivateKey())
            && !updatedOauthClient.getPrivateKey().equals(oauthClient.get().getPrivateKey())) {
            final PrivateKey privateKey;
            try {
                privateKey = generatePrivateKey(updatedOauthClient.getPrivateKey());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException(e.getMessage());
            }

            val privateCrtKey = (RSAPrivateCrtKey) privateKey;
            val publicKeySpec = new java.security.spec.RSAPublicKeySpec(privateCrtKey.getModulus(), privateCrtKey.getPublicExponent());
            KeyFactory keyFactory = null;
            try {
                keyFactory = KeyFactory.getInstance("RSA");
                val publicKey = keyFactory.generatePublic(publicKeySpec);
                val writer = new StringWriter();
                val pemWriter = new PemWriter(writer);
                pemWriter.writeObject(new PemObject("RSA PUBLIC KEY", publicKey.getEncoded()));
                pemWriter.flush();
                pemWriter.close();
                val publicKeyPem = writer.toString();

                oauthClientDao.update(
                        new OauthClient(
                                clientId,
                                oauthClient.get().getCreateTime(),
                                updatedOauthClient.getDescription(),
                                oauthClient.get().getSecret(),
                                updatedOauthClient.getGrantTypes(),
                                oauthClient.get().getOrganizationId(),
                                updatedOauthClient.isEnabled(),
                                updatedOauthClient.getRedirectUrls(),
                                updatedOauthClient.getExpiration(),
                                updatedOauthClient.getRefreshExpiration(),
                                updatedOauthClient.getTokenFormat(),
                                updatedOauthClient.getPrivateKey(),
                                publicKeyPem,
                                now,
                                oauthClient.get().getScopes(),
                                null
                        )
                );
            } catch (Exception e) {
                throw new BadRequestException("Unable to update OAuth2 client: " + e.getMessage());
            }
        } else {
            oauthClientDao.update(
                    new OauthClient(
                            clientId,
                            oauthClient.get().getCreateTime(),
                            updatedOauthClient.getDescription(),
                            oauthClient.get().getSecret(),
                            updatedOauthClient.getGrantTypes(),
                            oauthClient.get().getOrganizationId(),
                            updatedOauthClient.isEnabled(),
                            updatedOauthClient.getRedirectUrls(),
                            updatedOauthClient.getExpiration(),
                            updatedOauthClient.getRefreshExpiration(),
                            updatedOauthClient.getTokenFormat(),
                            oauthClient.get().getPrivateKey(),
                            oauthClient.get().getPublicKey(),
                            now,
                            oauthClient.get().getScopes(),
                            null
                    )
            );
        }

        val originalOauthScopeIdList = oauthClient.get().getScopes()
                .stream()
                .map(OauthScope::getId)
                .toList();

        if (updatedOauthClient.getScopeIds() != null) {
            // See if there are new scopes that needs to be created
            updatedOauthClient.getScopeIds()
                    .forEach(scopeId -> {
                        if (!originalOauthScopeIdList.contains(scopeId)) {
                            oauthClientScopeDao.insert(new OauthClientScope(
                                    createId(),
                                    now,
                                    clientId,
                                    scopeId
                            ));
                        }
                    });

            // See if there are old scopes which need to be removed
            oauthClient.get().getScopes()
                    .forEach(originalScope -> {
                        if (!updatedOauthClient.getScopeIds().contains(originalScope.getId())) {
                            oauthClientScopeDao.deleteByClientIdAndScopeId(clientId, originalScope.getId());
                        }
                    });
        }

        return OauthClientDto.fromEntity(oauthClientDao.getById(clientId).orElseThrow(clientNotFound(clientId)));
    }

    /**
     * Creates a new OAuth2 client for provided organization using specified update request object
     */
    @Override
    public OauthClientDto createOauth2Client(final Organization organization, final UpdateOauthClientRequest newOauthClient) {
        if (isEmpty(newOauthClient.getDescription())) {
            throw new BadRequestException("Client description can not be empty");
        }

        if (isEmpty(newOauthClient.getGrantTypes())) {
            throw new BadRequestException("Grant types list can not be empty");
        }

        if (newOauthClient.getGrantTypes().contains(authorization_code)
            && isEmpty(newOauthClient.getRedirectUrls())) {
            throw new BadRequestException("Redirect url list can not be empty when '" + authorization_code.name() + "' is selected");
        }

        if (isEmpty(newOauthClient.getExpiration())) {
            throw new BadRequestException("Token expiration can not be empty");
        }

        if (newOauthClient.getGrantTypes().contains(refresh_token)
            && isEmpty(newOauthClient.getRefreshExpiration() == null)) {
            throw new BadRequestException("Refresh token expiration can not be empty");
        }

        val rsaKeyPair = CertificateKeysUtils.generateRsaKeyPair();
        val now = Instant.now(defaultClock);
        val result = new OauthClient(
                createId(),
                now,
                newOauthClient.getDescription(),
                sha256(createId()),
                newOauthClient.getGrantTypes(),
                organization.getId(),
                true,
                newOauthClient.getRedirectUrls(),
                newOauthClient.getExpiration(),
                newOauthClient.getRefreshExpiration(),
                TokenFormat.STANDARD,
                rsaKeyPair.privateKeyPem,
                rsaKeyPair.publicKeyPem,
                now,
                null,
                null
        );

        oauthClientDao.insert(result);

        if (isNotEmpty(newOauthClient.getScopeIds())) {
            newOauthClient.getScopeIds()
                    .stream()
                    .parallel()
                    .forEach(scopeId -> oauthClientScopeDao.insert(new OauthClientScope(
                            createId(),
                            now,
                            result.getId(),
                            scopeId
                    )));
        }

        return OauthClientDto.fromEntity(result);
    }

    /**
     * Deletes one or more clients for provided organization using DeleteClientsRequest object
     */
    @Override
    public void deleteClients(final Organization organization, final DeleteClientsRequest deleteClientsRequest) {
        if (isEmpty(deleteClientsRequest.clientIds)) {
            throw new BadRequestException("Client IDs can not be empty");
        }

        deleteClientsRequest.clientIds.stream().parallel().forEach(clientId -> {
            val oauthClient = oauthClientDao.getById(clientId);
            if (oauthClient.isEmpty()) {
                throwClientNotFound(clientId);
            }

            if (!organization.getId().equals(oauthClient.get().getOrganizationId())) {
                throw new AccessDeniedException();
            }
            oauthClientDao.deleteById(oauthClient.get().getId());
            val oauthClientScopes = oauthClientScopeDao.listByClientId(oauthClient.get().getId());
            oauthClientScopes.stream().parallel().forEach(oauthClientScope -> oauthClientScopeDao.deleteById(oauthClientScope.getId()));
        });
    }

    /**
     * Creates new keys for provided organization and client id
     */
    @Override
    public OauthClientDto createNewKeys(final String clientId, final Organization organization) {
        val oauthClient = oauthClientDao.getById(clientId);
        if (oauthClient.isEmpty()) {
            throwClientNotFound(clientId);
        }

        if (!organization.getId().equals(oauthClient.get().getOrganizationId())) {
            throw new AccessDeniedException();
        }

        val rsaKeyPair = CertificateKeysUtils.generateRsaKeyPair();

        oauthClientDao.update(
                new OauthClient(
                        clientId,
                        oauthClient.get().getCreateTime(),
                        oauthClient.get().getDescription(),
                        oauthClient.get().getSecret(),
                        oauthClient.get().getGrantTypes(),
                        oauthClient.get().getOrganizationId(),
                        oauthClient.get().isEnabled(),
                        oauthClient.get().getRedirectUrls(),
                        oauthClient.get().getExpiration(),
                        oauthClient.get().getRefreshExpiration(),
                        oauthClient.get().getTokenFormat(),
                        rsaKeyPair.privateKeyPem,
                        rsaKeyPair.publicKeyPem,
                        Instant.now(defaultClock),
                        oauthClient.get().getScopes(),
                        null
                )
        );

        return OauthClientDto.fromEntity(oauthClientDao.getById(clientId).orElseThrow(clientNotFound(clientId)));
    }

    /**
     * Assigns new keys for provided organization and specified client id
     */
    @Override
    public OauthClientDto assignKeys(final String clientId, final Organization organization, final String publicKeyString, final String privateKeyString) {
        try {
            generatePrivateKey(privateKeyString.trim());
            generatePublicKey(publicKeyString.trim());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }

        val oauthClient = oauthClientDao.getById(clientId);
        if (oauthClient.isEmpty()) {
            throwClientNotFound(clientId);
        }

        if (!organization.getId().equals(oauthClient.get().getOrganizationId())) {
            throw new AccessDeniedException();
        }

        oauthClientDao.update(
                new OauthClient(
                        clientId,
                        oauthClient.get().getCreateTime(),
                        oauthClient.get().getDescription(),
                        oauthClient.get().getSecret(),
                        oauthClient.get().getGrantTypes(),
                        oauthClient.get().getOrganizationId(),
                        oauthClient.get().isEnabled(),
                        oauthClient.get().getRedirectUrls(),
                        oauthClient.get().getExpiration(),
                        oauthClient.get().getRefreshExpiration(),
                        oauthClient.get().getTokenFormat(),
                        privateKeyString.trim(),
                        publicKeyString.trim(),
                        Instant.now(defaultClock),
                        oauthClient.get().getScopes(),
                        null
                )
        );

        return OauthClientDto.fromEntity(oauthClientDao.getById(clientId).orElseThrow(clientNotFound(clientId)));
    }

    private void throwClientNotFound(final String clientId) {
        throw new EntityNotFoundException("Client not found by id: " + clientId);
    }

    private Supplier<EntityNotFoundException> clientNotFound(final String clientId) {
        return () -> new EntityNotFoundException("Client not found by id: " + clientId);
    }
}
