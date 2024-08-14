package com.authbox.web.service;

import com.authbox.base.config.AppProperties;
import com.authbox.base.dao.OauthClientDao;
import com.authbox.base.dao.OauthClientScopeDao;
import com.authbox.base.dao.OauthScopeDao;
import com.authbox.base.dao.OauthUserDao;
import com.authbox.base.dao.OrganizationDao;
import com.authbox.base.dao.UserDao;
import com.authbox.base.exception.BadRequestException;
import com.authbox.base.model.GrantType;
import com.authbox.base.model.OauthClient;
import com.authbox.base.model.OauthClientScope;
import com.authbox.base.model.OauthScope;
import com.authbox.base.model.OauthUser;
import com.authbox.base.model.Organization;
import com.authbox.base.model.TokenFormat;
import com.authbox.base.model.User;
import com.authbox.base.util.CertificateKeysUtils;
import com.authbox.web.model.CreateAccountWithOrganizationRequest;
import com.authbox.web.model.UserDto;
import com.authbox.web.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.val;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static com.authbox.base.util.HashUtils.makeRandomBase32;
import static com.authbox.base.util.HashUtils.sha256;
import static com.authbox.base.util.IdUtils.createId;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.util.ObjectUtils.isEmpty;

@Service
@AllArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {

    private final Clock defaultClock;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;
    private final UserDao userDao;
    private final OrganizationDao organizationDao;
    private final OauthScopeDao oauthScopeDao;
    private final OauthClientDao oauthClientDao;
    private final OauthClientScopeDao oauthClientScopeDao;
    private final OauthUserDao oauthUserDao;
    private final OrganizationService organizationService;

    /**
     * Creates a new account with new organization using CreateAccountWithOrganizationRequest object
     */
    @Override
    public UserDto createAccountWithOrganization(final CreateAccountWithOrganizationRequest request) {
        if (!appProperties.isRegistrationEnabled()) {
            throw new BadRequestException("Registration functionality is disabled");
        }
        if (isEmpty(request.username)) {
            throw new BadRequestException("Account username can not be empty");
        }
        if (isEmpty(request.name)) {
            throw new BadRequestException("Account name can not be empty");
        }
        if (isEmpty(request.password)) {
            throw new BadRequestException("Password can not be empty");
        }
        if (!request.password.equalsIgnoreCase(request.password2)) {
            throw new BadRequestException("Passwords do not match");
        }
        if (userDao.getByUsername(request.username.trim()).isPresent()) {
            throw new BadRequestException("This username is taken, please select another user");
        }
        if (isEmpty(request.domainPrefix)) {
            throw new BadRequestException("Domain prefix can not be empty");
        }
        if (isEmpty(request.organizationName)) {
            throw new BadRequestException("Organization name can not be empty");
        }

        val userId = createId();
        val organizationId = createId();
        val now = Instant.now(defaultClock);
        organizationService.validateDomainPrefix(request.domainPrefix, organizationId);

        val rsaKeyPair = CertificateKeysUtils.generateRsaKeyPair();

        organizationDao.insert(
                new Organization(
                        organizationId,
                        now,
                        request.organizationName.trim(),
                        request.domainPrefix.trim(),
                        "",
                        true,
                        isBlank(request.logoUrl) ? "" : request.logoUrl.trim(),
                        now
                )
        );

        userDao.insert(
                new User(
                        userId,
                        now,
                        request.username.trim(),
                        (isEmpty(request.password)
                                ? passwordEncoder.encode(createId())
                                : passwordEncoder.encode(request.password.trim())),
                        request.name.trim(),
                        List.of(UserRole.ROLE_ADMIN.name()),
                        true,
                        organizationId,
                        now
                )
        );

        val scopeId = createId();
        oauthScopeDao.insert(
                new OauthScope(
                        scopeId,
                        now,
                        "Ability to open and close a secret door",
                        "some/scope",
                        organizationId
                )
        );

        val clientId = createId();
        oauthClientDao.insert(
                new OauthClient(
                        clientId,
                        now,
                        "Sample service-to-service client",
                        sha256(createId()),
                        List.of(GrantType.client_credentials),
                        organizationId,
                        true,
                        List.of(),
                        Duration.ofHours(4),
                        Duration.ofHours(24),
                        TokenFormat.STANDARD,
                        "",
                        "",
                        now,
                        null,
                        null
                )
        );

        oauthClientScopeDao.insert(
                new OauthClientScope(
                        createId(),
                        now,
                        clientId,
                        scopeId
                )
        );

        val clientId2 = createId();
        oauthClientDao.insert(
                new OauthClient(
                        clientId2,
                        now,
                        "Sample user auth client",
                        sha256(createId()),
                        List.of(GrantType.password, GrantType.authorization_code, GrantType.refresh_token),
                        organizationId,
                        true,
                        List.of(
                                "https://some-domain/auth/callback",
                                "https://another-domain/auth/callback"
                        ),
                        Duration.ofHours(1),
                        Duration.ofHours(24),
                        TokenFormat.JWT,
                        rsaKeyPair.privateKeyPem,
                        rsaKeyPair.publicKeyPem,
                        now,
                        null,
                        null
                )
        );
        oauthClientScopeDao.insert(
                new OauthClientScope(
                        createId(),
                        now,
                        clientId2,
                        scopeId
                )
        );

        oauthUserDao.insert(
                new OauthUser(
                        createId(),
                        now,
                        "test",
                        passwordEncoder.encode("test"),
                        true,
                        organizationId,
                        "{\"someKey\":\"someValue\"}",
                        false,
                        makeRandomBase32(),
                        now
                )
        );

        return UserDto.fromEntity(
                userDao.getById(userId).orElseThrow(() -> new BadRequestException("User not found by id: " + userId))
        );
    }
}
