package com.authbox.web.controller;

import com.authbox.base.exception.AccessDeniedException;
import com.authbox.base.exception.BadRequestException;
import com.authbox.base.exception.EntityNotFoundException;
import com.authbox.base.model.OauthClient;
import com.authbox.base.model.OauthClientScope;
import com.authbox.base.model.OauthScope;
import com.authbox.base.model.Organization;
import com.authbox.base.model.RsaKeyPair;
import com.authbox.base.model.TokenFormat;
import com.authbox.base.util.CertificateKeysUtils;
import com.authbox.web.config.Constants;
import com.authbox.web.model.DeleteClientsRequest;
import io.swagger.annotations.ApiParam;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.transaction.Transactional;
import java.io.IOException;
import java.io.StringWriter;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static com.authbox.base.model.GrantType.authorization_code;
import static com.authbox.base.model.GrantType.refresh_token;
import static com.authbox.base.util.CertificateKeysUtils.generatePrivateKey;
import static com.authbox.base.util.CertificateKeysUtils.generatePublicKey;
import static com.authbox.base.util.HashUtils.sha256;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.util.ObjectUtils.isEmpty;

@RestController
@RequestMapping(Constants.API_PREFIX + "/oauth2-client")
public class Oauth2ClientsController extends BaseController {

    private final Clock defaultClock;

    public Oauth2ClientsController(final Clock defaultClock) {
        this.defaultClock = defaultClock;
        Security.addProvider(new BouncyCastleProvider());
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Page<OauthClient> getOauth2Clients(@RequestParam(value = "pageSize", defaultValue = "10") final int pageSize,
                                              @RequestParam(value = "currentPage", defaultValue = "0") final int currentPage) {
        final Organization organization = getOrganization();
        return oauthClientDao.listByOrganizationId(organization.getId(), PageRequest.of(currentPage, pageSize));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public OauthClient getOauth2ClientById(@PathVariable("id") final String id) {
        final Organization organization = getOrganization();

        final Optional<OauthClient> oauthClient = oauthClientDao.getById(id);
        if (oauthClient.isEmpty()) {
            throw new EntityNotFoundException("Client not found by id: " + id);
        }

        if (!organization.getId().equals(oauthClient.get().getOrganizationId())) {
            throw new AccessDeniedException();
        }

        return oauthClient.get();
    }

    @PostMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public OauthClient updateOauth2ClientById(@PathVariable("id") final String clientId,
                                              @RequestBody final OauthClient updatedOauthClient) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        final Organization organization = getOrganization();

        final Optional<OauthClient> oauthClient = oauthClientDao.getById(clientId);
        if (oauthClient.isEmpty()) {
            throw new EntityNotFoundException("Client not found by id: " + clientId);
        }

        if (!organization.getId().equals(oauthClient.get().getOrganizationId())) {
            throw new AccessDeniedException();
        }

        final Instant now = Instant.now(defaultClock);
        if (isNotBlank(updatedOauthClient.getPrivateKey())
                && !updatedOauthClient.getPrivateKey().equals(oauthClient.get().getPrivateKey())) {
            final PrivateKey privateKey;
            try {
                privateKey = generatePrivateKey(updatedOauthClient.getPrivateKey());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException(e.getMessage());
            }

            final RSAPrivateCrtKey privateCrtKey = (RSAPrivateCrtKey) privateKey;
            final RSAPublicKeySpec publicKeySpec = new java.security.spec.RSAPublicKeySpec(privateCrtKey.getModulus(), privateCrtKey.getPublicExponent());
            final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            final PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);
            final StringWriter writer = new StringWriter();
            final PemWriter pemWriter = new PemWriter(writer);
            pemWriter.writeObject(new PemObject("RSA PUBLIC KEY", publicKey.getEncoded()));
            pemWriter.flush();
            pemWriter.close();
            final String publicKeyPem = writer.toString();

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

        final List<String> originalOauthScopeIdList = oauthClient.get().getScopes()
                .stream()
                .map(OauthScope::getId)
                .collect(toUnmodifiableList());

        if (updatedOauthClient.getScopeIds() != null) {
            // See if there are new scopes that needs to be created
            updatedOauthClient.getScopeIds()
                    .forEach(scopeId -> {
                        if (!originalOauthScopeIdList.contains(scopeId)) {
                            oauthClientScopeDao.insert(new OauthClientScope(
                                    UUID.randomUUID().toString(),
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

        return oauthClientDao.getById(updatedOauthClient.getId()).orElseThrow((Supplier<EntityNotFoundException>) () -> {
            throw new EntityNotFoundException("Client not found by id: " + updatedOauthClient.getId());
        });
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public OauthClient createOauth2Client(@RequestBody final OauthClient updatedOauthClient) {
        final Organization organization = getOrganization();

        if (isEmpty(updatedOauthClient.getDescription())) {
            throw new BadRequestException("Client description can not be empty");
        }

        if (isEmpty(updatedOauthClient.getGrantTypes())) {
            throw new BadRequestException("Grant types list can not be empty");
        }

        if (updatedOauthClient.getGrantTypes().contains(authorization_code)
                && isEmpty(updatedOauthClient.getRedirectUrls())) {
            throw new BadRequestException("Redirect url list can not be empty when '" + authorization_code.name() + "' is selected");
        }

        if (isEmpty(updatedOauthClient.getExpiration())) {
            throw new BadRequestException("Token expiration can not be empty");
        }

        if (updatedOauthClient.getGrantTypes().contains(refresh_token)
                && isEmpty(updatedOauthClient.getRefreshExpiration() == null)) {
            throw new BadRequestException("Refresh token expiration can not be empty");
        }

        final RsaKeyPair rsaKeyPair = CertificateKeysUtils.generateRsaKeyPair();

        final Instant now = Instant.now(defaultClock);
        final OauthClient result = new OauthClient(
                UUID.randomUUID().toString(),
                now,
                updatedOauthClient.getDescription(),
                sha256(UUID.randomUUID().toString()),
                updatedOauthClient.getGrantTypes(),
                organization.getId(),
                true,
                updatedOauthClient.getRedirectUrls(),
                updatedOauthClient.getExpiration(),
                updatedOauthClient.getRefreshExpiration(),
                TokenFormat.STANDARD,
                rsaKeyPair.privateKeyPem,
                rsaKeyPair.publicKeyPem,
                now,
                null,
                null
        );

        oauthClientDao.insert(result);

        if (isNotEmpty(updatedOauthClient.getScopeIds())) {
            updatedOauthClient.getScopeIds()
                    .stream()
                    .parallel()
                    .forEach(scopeId -> oauthClientScopeDao.insert(new OauthClientScope(
                            UUID.randomUUID().toString(),
                            now,
                            result.getId(),
                            scopeId
                    )));
        }

        return result;
    }

    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    public void deleteClients(@RequestBody final DeleteClientsRequest deleteClientsRequest) {
        final Organization organization = getOrganization();

        if (isEmpty(deleteClientsRequest.clientIds)) {
            throw new BadRequestException("Client IDs can not be empty");
        }

        deleteClientsRequest.clientIds.stream().parallel().forEach(clientId -> {
            final Optional<OauthClient> oauthClient = oauthClientDao.getById(clientId);
            if (oauthClient.isEmpty()) {
                throw new EntityNotFoundException("Client not found by id: " + clientId);
            }

            if (!organization.getId().equals(oauthClient.get().getOrganizationId())) {
                throw new AccessDeniedException();
            }
            oauthClientDao.deleteById(oauthClient.get().getId());
            final List<OauthClientScope> oauthClientScopes = oauthClientScopeDao.listByClientId(oauthClient.get().getId());
            oauthClientScopes.stream().parallel().forEach(oauthClientScope -> oauthClientScopeDao.deleteById(oauthClientScope.getId()));
        });
    }

    @PostMapping("/{id}/create-new-keys")
    @PreAuthorize("hasAuthority('SCOPE_organization/write') OR isAuthenticated()")
    public OauthClient createNewKeys(@PathVariable("id") final String clientId) {
        final Organization organization = getOrganization();
        final Optional<OauthClient> oauthClient = oauthClientDao.getById(clientId);
        if (oauthClient.isEmpty()) {
            throw new EntityNotFoundException("Client not found by id: " + clientId);
        }

        if (!organization.getId().equals(oauthClient.get().getOrganizationId())) {
            throw new AccessDeniedException();
        }

        final RsaKeyPair rsaKeyPair = CertificateKeysUtils.generateRsaKeyPair();

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

        return oauthClientDao.getById(clientId).orElseThrow(() -> new EntityNotFoundException("Client not found by id: " + clientId));
    }

    @PostMapping(value = "/{id}/assign-keys", consumes = "application/x-www-form-urlencoded")
    @PreAuthorize("hasAuthority('SCOPE_organization/write') OR isAuthenticated()")
    public OauthClient assignKeys(
            @PathVariable("id") final String clientId,
            @ApiParam(allowMultiple = true, format = "text", required = true)
            @RequestParam("publicKey") final String publicKeyString,
            @ApiParam(allowMultiple = true, format = "text", required = true)
            @RequestParam("privateKey") final String privateKeyString) {
        final Organization organization = getOrganization();

        try {
            generatePrivateKey(privateKeyString.trim());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }
        try {
            generatePublicKey(publicKeyString.trim());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }

        final Optional<OauthClient> oauthClient = oauthClientDao.getById(clientId);
        if (oauthClient.isEmpty()) {
            throw new EntityNotFoundException("Client not found by id: " + clientId);
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

        return oauthClientDao.getById(clientId).orElseThrow(() -> new EntityNotFoundException("Client not found by id: " + clientId));
    }
}
