package com.authbox.web.controller;

import com.authbox.base.exception.AccessDeniedException;
import com.authbox.base.exception.BadRequestException;
import com.authbox.base.exception.EntityNotFoundException;
import com.authbox.base.model.OauthUser;
import com.authbox.base.model.Organization;
import com.authbox.base.util.HashUtils;
import com.authbox.web.model.DeleteUsersRequest;
import com.authbox.web.model.PasswordChangeRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static com.authbox.web.config.Constants.API_PREFIX;
import static org.springframework.http.MediaType.IMAGE_PNG_VALUE;
import static org.springframework.util.ObjectUtils.isEmpty;

@RestController
@RequestMapping(API_PREFIX + "/oauth2-user")
public class Oauth2UsersController extends BaseController {

    private final PasswordEncoder passwordEncoder;
    private final RestTemplate restTemplate;
    private final Clock defaultClock;

    public Oauth2UsersController(final PasswordEncoder passwordEncoder, final RestTemplate restTemplate, final Clock defaultClock) {
        this.passwordEncoder = passwordEncoder;
        this.restTemplate = restTemplate;
        this.defaultClock = defaultClock;
    }

    @GetMapping
    public Page<OauthUser> getOauth2Users(@RequestParam(value = "pageSize", defaultValue = "10") final int pageSize,
                                          @RequestParam(value = "currentPage", defaultValue = "0") final int currentPage) {
        final Organization organization = getOrganization();
        return oauthUserDao.listByOrganizationId(organization.id, PageRequest.of(currentPage, pageSize));
    }

    @GetMapping("/{id}")
    public OauthUser getOauth2UserById(@PathVariable("id") final String id) {
        final Organization organization = getOrganization();

        final Optional<OauthUser> oauthUser = oauthUserDao.getById(id);
        if (oauthUser.isEmpty()) {
            throw new EntityNotFoundException("User not found by id: " + id);
        }

        if (!organization.id.equals(oauthUser.get().organizationId)) {
            throw new AccessDeniedException();
        }

        return oauthUser.get();
    }

    @PostMapping("/{id}/password-reset")
    public OauthUser updatePassword(@PathVariable("id") final String userId,
                                    @RequestBody final PasswordChangeRequest passwordChangeRequest) {
        final Organization organization = getOrganization();
        final Optional<OauthUser> oauthUser = oauthUserDao.getById(userId);
        if (oauthUser.isEmpty()) {
            throw new EntityNotFoundException("User not found by id: " + userId);
        }

        if (!organization.id.equals(oauthUser.get().organizationId)) {
            throw new AccessDeniedException();
        }

        // verify new password and password2 match
        if (!passwordChangeRequest.newPassword.equals(passwordChangeRequest.newPassword2)) {
            throw new BadRequestException("New password and new password 2 do not match");
        }

        oauthUserDao.update(
                userId,
                oauthUser.get().username,
                isEmpty(passwordChangeRequest.newPassword) // if empty set to random
                        ? passwordEncoder.encode(UUID.randomUUID().toString())
                        : passwordEncoder.encode(passwordChangeRequest.newPassword),
                oauthUser.get().enabled,
                oauthUser.get().metadata,
                oauthUser.get().using2Fa,
                Instant.now(defaultClock)
        );

        return oauthUserDao.getById(userId).orElseThrow((Supplier<EntityNotFoundException>) () -> {
            throw new EntityNotFoundException("User not found by id: " + userId);
        });
    }

    @GetMapping(value = "/{id}/2fa-qr-code", produces = IMAGE_PNG_VALUE)
    public byte[] generate2FaQrCodeImage(@PathVariable("id") final String userId) {
        final Organization organization = getOrganization();
        final Optional<OauthUser> oauthUser = oauthUserDao.getById(userId);
        if (oauthUser.isEmpty()) {
            throw new EntityNotFoundException("User not found by id: " + userId);
        }

        if (!organization.id.equals(oauthUser.get().organizationId)) {
            throw new AccessDeniedException();
        }

        final String qrCodeUrl = "https://chart.googleapis.com/chart?chs=200x200&chld=M%%7C0&cht=qr&chl=otpauth://totp/"
                + organization.name
                + " ("
                + oauthUser.get().username
                + ")?secret="
                + oauthUser.get().secret
                + "&issuer=auth-box";

        return restTemplate.getForObject(qrCodeUrl, byte[].class);
    }

    @PostMapping("/{id}")
    public OauthUser updateOauth2UserById(@PathVariable("id") final String userId,
                                          @RequestBody final OauthUser updatedOauthUser) {
        final Organization organization = getOrganization();

        final Optional<OauthUser> oauthUser = oauthUserDao.getById(userId);
        if (oauthUser.isEmpty()) {
            throw new EntityNotFoundException("User not found by id: " + userId);
        }

        if (!organization.id.equals(oauthUser.get().organizationId) || !oauthUser.get().id.equals(updatedOauthUser.id)) {
            throw new AccessDeniedException();
        }

        // check if username changed
        if (!oauthUser.get().username.equals(updatedOauthUser.username.trim())) {
            // check if username already there
            if (oauthUserDao.getByUsernameAndOrganizationId(updatedOauthUser.username.trim(), organization.id).isPresent()) {
                throw new BadRequestException("Username already exists: " + updatedOauthUser.username.trim());
            }
        }

        final Instant now = Instant.now(defaultClock);

        if (updatedOauthUser.enabled != oauthUser.get().enabled) {
            oauthUserDao.update(
                    oauthUser.get().id,
                    oauthUser.get().username,
                    oauthUser.get().password,
                    updatedOauthUser.enabled,
                    oauthUser.get().metadata,
                    oauthUser.get().using2Fa,
                    now
            );
        } else {
            oauthUserDao.update(
                    oauthUser.get().id,
                    updatedOauthUser.username,
                    oauthUser.get().password,
                    updatedOauthUser.enabled,
                    updatedOauthUser.metadata,
                    updatedOauthUser.using2Fa,
                    now
            );
        }

        return oauthUserDao.getById(oauthUser.get().id).orElseThrow((Supplier<EntityNotFoundException>) () -> {
            throw new EntityNotFoundException("User not found by id: " + oauthUser.get().id);
        });
    }

    @PostMapping
    public OauthUser createOauth2User(@RequestBody final OauthUser newOauthUser) {
        final Organization organization = getOrganization();

        if (isEmpty(newOauthUser.username)) {
            throw new BadRequestException("Username can not be empty");
        }
        final String password;
        if (isEmpty(newOauthUser.password)) {
            password = passwordEncoder.encode(UUID.randomUUID().toString());
        } else {
            password = passwordEncoder.encode(newOauthUser.password.trim());
        }

        // check if username already there
        if (oauthUserDao.getByUsernameAndOrganizationId(newOauthUser.username.trim(), organization.id).isPresent()) {
            throw new BadRequestException("Username already exists: " + newOauthUser.username.trim());
        }

        final String id = UUID.randomUUID().toString();
        final Instant now = Instant.now(defaultClock);

        final OauthUser result = new OauthUser(
                id,
                now,
                newOauthUser.username.trim(),
                password,
                true,
                organization.id,
                isEmpty(newOauthUser.metadata) ? "" : newOauthUser.metadata.trim(),
                newOauthUser.using2Fa,
                HashUtils.makeRandomBase32(),
                now
        );

        oauthUserDao.insert(result);

        return result;
    }

    @DeleteMapping
    public void deleteUsers(@RequestBody final DeleteUsersRequest deleteUsersRequest) {
        final Organization organization = getOrganization();

        if (isEmpty(deleteUsersRequest.userIds)) {
            throw new BadRequestException("User IDs can not be empty");
        }

        deleteUsersRequest.userIds.stream().parallel().forEach(userId -> {
            final Optional<OauthUser> oauthUser = oauthUserDao.getById(userId);
            if (oauthUser.isEmpty()) {
                throw new EntityNotFoundException("User not found by id: " + userId);
            }

            if (!organization.id.equals(oauthUser.get().organizationId)) {
                throw new AccessDeniedException();
            }
            oauthUserDao.deleteById(oauthUser.get().id);
        });
    }
}