package com.authbox.web.service;

import com.authbox.base.dao.OauthUserDao;
import com.authbox.base.exception.AccessDeniedException;
import com.authbox.base.exception.BadRequestException;
import com.authbox.base.exception.EntityNotFoundException;
import com.authbox.base.model.OauthUser;
import com.authbox.base.model.Organization;
import com.authbox.base.model.UpdateOauthUserRequest;
import com.authbox.base.util.HashUtils;
import com.authbox.web.model.DeleteUsersRequest;
import com.authbox.web.model.PasswordChangeRequest;
import com.google.zxing.WriterException;
import lombok.AllArgsConstructor;
import lombok.val;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.time.Clock;
import java.time.Instant;
import java.util.function.Supplier;

import static com.authbox.base.util.IdUtils.createId;
import static org.springframework.util.ObjectUtils.isEmpty;

@Service
@AllArgsConstructor
public class Oauth2UsersServiceImpl implements Oauth2UsersService {

    private final Clock defaultClock;
    private final OauthUserDao oauthUserDao;
    private final PasswordEncoder passwordEncoder;
    private final QrCodeGeneratorService qrCodeGeneratorService;

    /**
     * Returns paginated list of OauthUserDto objects for provided organization
     */
    @Override
    public Page<OauthUserDto> getOauth2Users(final Organization organization, final int pageSize, final int currentPage) {
        return oauthUserDao.listByOrganizationId(organization.getId(), PageRequest.of(currentPage, pageSize))
                .map(OauthUserDto::fromEntity);
    }

    /**
     * Returns OauthUserDto object for provided organization by id
     */
    @Override
    public OauthUserDto getOauth2UserById(final Organization organization, final String id) {
        val oauthUser = oauthUserDao.getById(id);
        if (oauthUser.isEmpty()) {
            throwUserNotFound(id);
        }
        if (!organization.getId().equals(oauthUser.get().getOrganizationId())) {
            throw new AccessDeniedException();
        }
        return OauthUserDto.fromEntity(oauthUser.get());
    }

    /**
     * Update password for specified user and provided organization using PasswordChangeRequest object
     */
    @Override
    public OauthUserDto updatePassword(final Organization organization, final String userId, final PasswordChangeRequest passwordChangeRequest) {
        val oauthUser = oauthUserDao.getById(userId);
        if (oauthUser.isEmpty()) {
            throwUserNotFound(userId);
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
                        ? passwordEncoder.encode(createId())
                        : passwordEncoder.encode(passwordChangeRequest.newPassword),
                oauthUser.get().isEnabled(),
                oauthUser.get().getMetadata(),
                oauthUser.get().isUsing2Fa(),
                Instant.now(defaultClock)
        );
        return OauthUserDto.fromEntity(
                oauthUserDao.getById(userId).orElseThrow((Supplier<EntityNotFoundException>) () -> {
                    throw new EntityNotFoundException("User not found by id: " + userId);
                })
        );
    }

    /**
     * Generates QR code for 2FA for user with provided organization
     */
    @Override
    public BufferedImage generate2FaQrCodeImage(final Organization organization, final String userId) {
        val oauthUser = oauthUserDao.getById(userId);
        if (oauthUser.isEmpty()) {
            throwUserNotFound(userId);
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

        try {
            return qrCodeGeneratorService.generateQrCode(qrCodeUrl);
        } catch (WriterException e) {
            throw new BadRequestException("Unable to create QR code: " + e.getMessage());
        }
    }

    /**
     * Updates user with provided organization using UpdateOauthUserRequest
     */
    @Override
    public OauthUserDto updateOauth2UserById(final Organization organization, final String userId, final UpdateOauthUserRequest updatedOauthUser) {
        val oauthUser = oauthUserDao.getById(userId);
        if (oauthUser.isEmpty()) {
            throwUserNotFound(userId);
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
        return OauthUserDto.fromEntity(
                oauthUserDao.getById(oauthUser.get().getId()).orElseThrow((Supplier<EntityNotFoundException>) () -> {
                    throw new EntityNotFoundException("User not found by id: " + oauthUser.get().getId());
                })
        );
    }

    /**
     * Creates a new user for provided organization using UpdateOauthUserRequest object
     */
    @Override
    public OauthUserDto createOauth2User(final Organization organization, final UpdateOauthUserRequest newOauthUser) {
        if (isEmpty(newOauthUser.getUsername())) {
            throw new BadRequestException("Username can not be empty");
        }
        // check if username already there
        if (oauthUserDao.getByUsernameAndOrganizationId(newOauthUser.getUsername().trim(), organization.getId()).isPresent()) {
            throw new BadRequestException("Username already exists: " + newOauthUser.getUsername().trim());
        }
        val password = isEmpty(newOauthUser.getPassword()) ?
                passwordEncoder.encode(createId())
                : passwordEncoder.encode(newOauthUser.getPassword().trim());
        val id = createId();
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
        return OauthUserDto.fromEntity(result);
    }

    /**
     * Deletes user for provided organization using DeleteUsersRequest object
     */
    @Override
    public void deleteUsers(final Organization organization, final DeleteUsersRequest deleteUsersRequest) {
        if (isEmpty(deleteUsersRequest.userIds)) {
            throw new BadRequestException("User IDs can not be empty");
        }
        deleteUsersRequest.userIds.stream().parallel().forEach(userId -> {
            val oauthUser = oauthUserDao.getById(userId);
            if (oauthUser.isEmpty()) {
                throwUserNotFound(userId);
            }
            if (!organization.getId().equals(oauthUser.get().getOrganizationId())) {
                throw new AccessDeniedException();
            }
            oauthUserDao.deleteById(oauthUser.get().getId());
        });
    }

    private void throwUserNotFound(final String userId) {
        throw new EntityNotFoundException("User not found by id: " + userId);
    }
}
