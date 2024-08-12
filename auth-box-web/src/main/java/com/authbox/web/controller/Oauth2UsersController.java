package com.authbox.web.controller;

import com.authbox.base.exception.AccessDeniedException;
import com.authbox.base.exception.BadRequestException;
import com.authbox.base.exception.EntityNotFoundException;
import com.authbox.base.model.OauthUser;
import com.authbox.base.model.UpdateOauthUserRequest;
import com.authbox.base.util.HashUtils;
import com.authbox.web.model.DeleteUsersRequest;
import com.authbox.web.model.PasswordChangeRequest;
import com.authbox.web.service.QrCodeGeneratorService;
import com.google.zxing.WriterException;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.val;
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

import java.awt.image.BufferedImage;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;

import static com.authbox.web.config.Constants.API_PREFIX;
import static org.springframework.http.MediaType.IMAGE_PNG_VALUE;
import static org.springframework.util.ObjectUtils.isEmpty;

@RestController
@AllArgsConstructor
@RequestMapping(API_PREFIX + "/oauth2-user")
public class Oauth2UsersController extends BaseController {

    private final PasswordEncoder passwordEncoder;
    private final QrCodeGeneratorService qrCodeGeneratorService;
    private final Clock defaultClock;

    @GetMapping
    public Page<OauthUser> getOauth2Users(@RequestParam(value = "pageSize", defaultValue = "10") final int pageSize,
                                          @RequestParam(value = "currentPage", defaultValue = "0") final int currentPage) {
        val organization = getOrganization();
        return oauthUserDao.listByOrganizationId(organization.getId(), PageRequest.of(currentPage, pageSize));
    }

    @GetMapping("/{id}")
    public OauthUser getOauth2UserById(@PathVariable("id") final String id) {
        val organization = getOrganization();
        val oauthUser = oauthUserDao.getById(id);
        if (oauthUser.isEmpty()) {
            throw new EntityNotFoundException("User not found by id: " + id);
        }

        if (!organization.getId().equals(oauthUser.get().getOrganizationId())) {
            throw new AccessDeniedException();
        }

        return oauthUser.get();
    }

    @PostMapping("/{id}/password-reset")
    public OauthUser updatePassword(@PathVariable("id") final String userId,
                                    @RequestBody final PasswordChangeRequest passwordChangeRequest) {
        val organization = getOrganization();
        val oauthUser = oauthUserDao.getById(userId);
        if (oauthUser.isEmpty()) {
            throw new EntityNotFoundException("User not found by id: " + userId);
        }

        if (!organization.getId().equals(oauthUser.get().getOrganizationId())) {
            throw new AccessDeniedException();
        }

        // verify new password and password2 match
        if (!passwordChangeRequest.newPassword.equals(passwordChangeRequest.newPassword2)) {
            throw new BadRequestException("New password and new password 2 do not match");
        }

        oauthUserDao.update(
                userId,
                oauthUser.get().getUsername(),
                isEmpty(passwordChangeRequest.newPassword) // if empty set to random
                        ? passwordEncoder.encode(UUID.randomUUID().toString())
                        : passwordEncoder.encode(passwordChangeRequest.newPassword),
                oauthUser.get().isEnabled(),
                oauthUser.get().getMetadata(),
                oauthUser.get().isUsing2Fa(),
                Instant.now(defaultClock)
        );

        return oauthUserDao.getById(userId).orElseThrow((Supplier<EntityNotFoundException>) () -> {
            throw new EntityNotFoundException("User not found by id: " + userId);
        });
    }

    @GetMapping(value = "/{id}/2fa-qr-code", produces = IMAGE_PNG_VALUE)
    public BufferedImage generate2FaQrCodeImage(@PathVariable("id") final String userId) throws WriterException {
        val organization = getOrganization();
        val oauthUser = oauthUserDao.getById(userId);
        if (oauthUser.isEmpty()) {
            throw new EntityNotFoundException("User not found by id: " + userId);
        }

        if (!organization.getId().equals(oauthUser.get().getOrganizationId())) {
            throw new AccessDeniedException();
        }

        val qrCodeUrl = "otpauth://totp/"
                        + organization.getName()
                        + " ("
                        + oauthUser.get().getUsername()
                        + ")?secret="
                        + oauthUser.get().getSecret()
                        + "&issuer=auth-box";

        return qrCodeGeneratorService.generateQrCode(qrCodeUrl);
    }

    @PostMapping("/{id}")
    @Transactional
    public OauthUser updateOauth2UserById(@PathVariable("id") final String userId,
                                          @RequestBody final UpdateOauthUserRequest updatedOauthUser) {
        val organization = getOrganization();

        val oauthUser = oauthUserDao.getById(userId);
        if (oauthUser.isEmpty()) {
            throw new EntityNotFoundException("User not found by id: " + userId);
        }

        if (!organization.getId().equals(oauthUser.get().getOrganizationId()) || !oauthUser.get().getId().equals(updatedOauthUser.getId())) {
            throw new AccessDeniedException();
        }

        // check if username changed
        if (!oauthUser.get().getUsername().equals(updatedOauthUser.getUsername().trim())) {
            // check if username already there
            if (oauthUserDao.getByUsernameAndOrganizationId(updatedOauthUser.getUsername().trim(), organization.getId()).isPresent()) {
                throw new BadRequestException("Username already exists: " + updatedOauthUser.getUsername().trim());
            }
        }

        val now = Instant.now(defaultClock);

        if (updatedOauthUser.isEnabled() != oauthUser.get().isEnabled()) {
            oauthUserDao.update(
                    oauthUser.get().getId(),
                    oauthUser.get().getUsername(),
                    oauthUser.get().getPassword(),
                    updatedOauthUser.isEnabled(),
                    oauthUser.get().getMetadata(),
                    oauthUser.get().isUsing2Fa(),
                    now
            );
        } else {
            oauthUserDao.update(
                    oauthUser.get().getId(),
                    updatedOauthUser.getUsername(),
                    oauthUser.get().getPassword(),
                    updatedOauthUser.isEnabled(),
                    updatedOauthUser.getMetadata(),
                    updatedOauthUser.isUsing2Fa(),
                    now
            );
        }

        return oauthUserDao.getById(oauthUser.get().getId()).orElseThrow((Supplier<EntityNotFoundException>) () -> {
            throw new EntityNotFoundException("User not found by id: " + oauthUser.get().getId());
        });
    }

    @PostMapping
    public OauthUser createOauth2User(@RequestBody final UpdateOauthUserRequest newOauthUser) {
        val organization = getOrganization();

        if (isEmpty(newOauthUser.getUsername())) {
            throw new BadRequestException("Username can not be empty");
        }

        // check if username already there
        if (oauthUserDao.getByUsernameAndOrganizationId(newOauthUser.getUsername().trim(), organization.getId()).isPresent()) {
            throw new BadRequestException("Username already exists: " + newOauthUser.getUsername().trim());
        }

        final String password;
        if (isEmpty(newOauthUser.getPassword())) {
            password = passwordEncoder.encode(UUID.randomUUID().toString());
        } else {
            password = passwordEncoder.encode(newOauthUser.getPassword().trim());
        }

        val id = UUID.randomUUID().toString();
        val now = Instant.now(defaultClock);

        val result = new OauthUser(
                id,
                now,
                newOauthUser.getUsername().trim(),
                password,
                true,
                organization.getId(),
                isEmpty(newOauthUser.getMetadata()) ? "" : newOauthUser.getMetadata().trim(),
                newOauthUser.isUsing2Fa(),
                HashUtils.makeRandomBase32(),
                now
        );

        oauthUserDao.insert(result);

        return result;
    }

    @DeleteMapping
    public void deleteUsers(@RequestBody final DeleteUsersRequest deleteUsersRequest) {
        val organization = getOrganization();

        if (isEmpty(deleteUsersRequest.userIds)) {
            throw new BadRequestException("User IDs can not be empty");
        }

        deleteUsersRequest.userIds.stream().parallel().forEach(userId -> {
            val oauthUser = oauthUserDao.getById(userId);
            if (oauthUser.isEmpty()) {
                throw new EntityNotFoundException("User not found by id: " + userId);
            }

            if (!organization.getId().equals(oauthUser.get().getOrganizationId())) {
                throw new AccessDeniedException();
            }
            oauthUserDao.deleteById(oauthUser.get().getId());
        });
    }
}