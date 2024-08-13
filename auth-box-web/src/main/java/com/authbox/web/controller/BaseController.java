package com.authbox.web.controller;

import com.authbox.base.config.AppProperties;
import com.authbox.base.dao.OauthClientDao;
import com.authbox.base.dao.OauthClientScopeDao;
import com.authbox.base.dao.OauthScopeDao;
import com.authbox.base.dao.OauthTokenDao;
import com.authbox.base.dao.OauthUserDao;
import com.authbox.base.dao.OrganizationDao;
import com.authbox.base.dao.UserDao;
import com.authbox.base.exception.AccessDeniedException;
import com.authbox.base.exception.BadRequestException;
import com.authbox.base.model.Organization;
import com.authbox.base.model.User;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;

import static com.authbox.base.config.Constants.OAUTH2_ATTR_ORGANIZATION_ID;
import static com.authbox.web.config.RequestWrapperFilterConfiguration.REQUEST_ORGANIZATION_MDC_KEY;

@Import(AppProperties.class)
@Slf4j
public class BaseController {

    @Autowired
    protected OrganizationDao organizationDao;
    @Autowired
    protected UserDao userDao;
    @Autowired
    protected OauthClientDao oauthClientDao;
    @Autowired
    protected OauthUserDao oauthUserDao;
    @Autowired
    protected OauthClientScopeDao oauthClientScopeDao;
    @Autowired
    protected OauthScopeDao oauthScopeDao;
    @Autowired
    protected OauthTokenDao oauthTokenDao;

    /**
     * Both Client/Password/AuthCode tokens as well, as standard logged-in User
     * are able to fetch organization they belong to.
     *
     * @return Organization object
     */
    public Organization getOrganization() {
        val details = getTokenAttributes(SecurityContextHolder.getContext().getAuthentication());
        val organizationId = Optional.ofNullable(details.get(OAUTH2_ATTR_ORGANIZATION_ID));

        if (organizationId.isEmpty()) {
            throw new BadRequestException("Token details do not contain organization_id");
        }
        return getOrganization(organizationId.get().toString());
    }

    Organization getOrganization(final String id) {
        val organization = organizationDao.getById(id);
        if (organization.isEmpty()) {
            throw new BadRequestException("Organization not found");
        }
        MDC.put(REQUEST_ORGANIZATION_MDC_KEY, organization.get().getId());
        return organization.get();
    }

    /**
     * TODO: Check
     * Does not work for oauth2 tokens, only for logged in users, since those are User and not Oauth2User types.
     */
    public User getCurrentUser() {
        val authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof AbstractAuthenticationToken && authentication.getPrincipal() instanceof User) {
            val userId = ((User) authentication.getPrincipal()).getId();
            return userDao.getById(userId).orElseThrow(() -> new BadRequestException("User not found by id: " + userId));
        } else {
            throw new BadRequestException("User details not available");
        }
    }

    public User getCurrentUserVerifyAdmin() {
        val user = getCurrentUser();
        if (!user.isAdmin()) {
            throw new AccessDeniedException();
        }
        return user;
    }

    private Map<String, Object> getTokenAttributes(final Principal principal) {
        if (principal instanceof BearerTokenAuthentication) {
            return ((BearerTokenAuthentication) principal).getTokenAttributes();
        } else if (principal instanceof AbstractAuthenticationToken) {
            if (((AbstractAuthenticationToken) principal).getPrincipal() instanceof DefaultOAuth2User) {
                return ((DefaultOAuth2User) ((AbstractAuthenticationToken) principal).getPrincipal()).getAttributes();
            } else {
                val user = (User) ((AbstractAuthenticationToken) principal).getPrincipal();
                return ImmutableMap.of(OAUTH2_ATTR_ORGANIZATION_ID, user.getOrganizationId());
            }

        } else {
            log.error("Unknown principal provided: {}", principal);
            throw new BadRequestException("Unknown principal");
        }
    }
}
