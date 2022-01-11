package com.authbox.web.controller;

import com.authbox.base.config.AppProperties;
import com.authbox.base.exception.BadRequestException;
import com.authbox.base.model.GrantType;
import com.authbox.base.model.OauthClient;
import com.authbox.base.model.OauthClientScope;
import com.authbox.base.model.OauthScope;
import com.authbox.base.model.OauthUser;
import com.authbox.base.model.Organization;
import com.authbox.base.model.RsaKeyPair;
import com.authbox.base.model.TokenFormat;
import com.authbox.base.model.User;
import com.authbox.base.util.CertificateKeysUtils;
import com.authbox.web.model.CreateAccountWithOrganizationRequest;
import com.authbox.web.model.UserRole;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import static com.authbox.base.util.HashUtils.makeRandomBase32;
import static com.authbox.base.util.HashUtils.sha256;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.util.ObjectUtils.isEmpty;

@RestController
@RequestMapping("/registration")
public class RegistrationController extends BaseController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistrationController.class);

    private final Clock defaultClock;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    public RegistrationController(final Clock defaultClock, final PasswordEncoder passwordEncoder, final AppProperties appProperties) {
        this.defaultClock = defaultClock;
        this.passwordEncoder = passwordEncoder;
        this.appProperties = appProperties;
    }

    @PostMapping
    public User createAccountWithOrganization(@RequestBody final CreateAccountWithOrganizationRequest request) {
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

        final String userId = randomUUID().toString();
        final String organizationId = randomUUID().toString();
        final Instant now = Instant.now(defaultClock);
        OrganizationController.validateDomainPrefix(organizationDao, request.domainPrefix, organizationId);

        final RsaKeyPair rsaKeyPair = CertificateKeysUtils.generateRsaKeyPair();

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
                                ? passwordEncoder.encode(randomUUID().toString())
                                : passwordEncoder.encode(request.password.trim())),
                        request.name.trim(),
                        ImmutableList.of(UserRole.ROLE_ADMIN.name()),
                        true,
                        organizationId,
                        now
                )
        );

        final String scopeId = randomUUID().toString();
        oauthScopeDao.insert(
                new OauthScope(
                        scopeId,
                        now,
                        "Sample scope",
                        "some/scope",
                        organizationId
                )
        );

        final String clientId = randomUUID().toString();
        oauthClientDao.insert(
                new OauthClient(
                        clientId,
                        now,
                        "Sample service-to-service client",
                        sha256(randomUUID().toString()),
                        ImmutableList.of(GrantType.client_credentials),
                        organizationId,
                        true,
                        ImmutableList.of(),
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
                        randomUUID().toString(),
                        now,
                        clientId,
                        scopeId
                )
        );

        final String clientId2 = randomUUID().toString();
        oauthClientDao.insert(
                new OauthClient(
                        clientId2,
                        now,
                        "Sample user auth client",
                        sha256(randomUUID().toString()),
                        ImmutableList.of(GrantType.password, GrantType.authorization_code, GrantType.refresh_token),
                        organizationId,
                        true,
                        ImmutableList.of(
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
                        randomUUID().toString(),
                        now,
                        clientId2,
                        scopeId
                )
        );

        oauthUserDao.insert(
                new OauthUser(
                        randomUUID().toString(),
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

        return userDao.getById(userId).orElseThrow(() -> new BadRequestException("User not found by id: " + userId));
    }
}
