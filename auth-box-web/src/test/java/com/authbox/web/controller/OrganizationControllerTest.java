package com.authbox.web.controller;

import com.authbox.base.dao.OrganizationDao;
import com.authbox.base.exception.BadRequestException;
import com.authbox.base.model.Organization;
import com.authbox.web.service.OrganizationServiceImpl;
import com.google.common.collect.ImmutableMap;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.util.AbstractMap.SimpleEntry;
import java.util.Optional;

import static com.authbox.base.config.Constants.OAUTH2_ATTR_ORGANIZATION_ID;
import static com.authbox.base.util.IdUtils.createId;
import static java.time.Instant.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OrganizationControllerTest {

    private static final String ORG_ID = createId();
    private static final String DOMAIN_PREFIX_ERROR_MSG = "Domain prefix can only contain letters and numbers";

    @Test
    public void testOrganizationController_checkAvailableDomainPrefix() {
        val organizationDao = mock(OrganizationDao.class);
        val organization = new Organization(ORG_ID, now(), "Test Org", "test", "", true, "", now());
        when(organizationDao.getById(ORG_ID)).thenReturn(Optional.of(organization));
        when(organizationDao.getByDomainPrefix(any(String.class))).thenReturn(Optional.empty());
        when(organizationDao.getByDomainPrefix("something")).thenReturn(Optional.of(organization));
        val organizationService = new OrganizationServiceImpl(Clock.systemUTC(), organizationDao);
        val controller = new OrganizationController(organizationService);

        // Need to set the OrganizationDao in base class of OrganizationController - BaseController
        ReflectionTestUtils.setField(controller, BaseController.class, "organizationDao", organizationDao, OrganizationDao.class);

        val securityContext = mock(SecurityContext.class);
        val authentication = mock(BearerTokenAuthentication.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getTokenAttributes()).thenReturn(ImmutableMap.of(OAUTH2_ATTR_ORGANIZATION_ID, ORG_ID));
        try (val mockedStatic = Mockito.mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            assertThat(controller.checkAvailableDomainPrefix("something")).contains(new SimpleEntry<>("exists", true));
            assertThat(controller.checkAvailableDomainPrefix("other")).contains(new SimpleEntry<>("exists", false));
            assertThatThrownBy(() -> controller.checkAvailableDomainPrefix("has space"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining(DOMAIN_PREFIX_ERROR_MSG);
            assertThatThrownBy(() -> controller.checkAvailableDomainPrefix("has,comma"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining(DOMAIN_PREFIX_ERROR_MSG);
            assertThatThrownBy(() -> controller.checkAvailableDomainPrefix("has.dot"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining(DOMAIN_PREFIX_ERROR_MSG);
            assertThatThrownBy(() -> controller.checkAvailableDomainPrefix("has_underscore"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining(DOMAIN_PREFIX_ERROR_MSG);
            assertThatThrownBy(() -> controller.checkAvailableDomainPrefix("has/slash"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining(DOMAIN_PREFIX_ERROR_MSG);
            assertThatThrownBy(() -> controller.checkAvailableDomainPrefix(""))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining(DOMAIN_PREFIX_ERROR_MSG);
        }
    }
}