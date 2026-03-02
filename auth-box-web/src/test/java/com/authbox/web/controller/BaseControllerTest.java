package com.authbox.web.controller;

import com.authbox.base.exception.BadRequestException;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BaseControllerTest {

    @Test
    public void testGetTokenAttributes() {
        val controller = new BaseController();
        val principal = mock(AbstractAuthenticationToken.class);
        val oAuth2User = mock(DefaultOAuth2User.class);
        when(oAuth2User.getAttributes()).thenReturn(Map.of("key", "value"));

        when(principal.getPrincipal()).thenReturn(oAuth2User);
        var mapResponse = controller.getTokenAttributes(principal);
        assertThat(mapResponse).isNotEmpty().containsEntry("key", "value");

        val token = mock(TestingAuthenticationToken.class);
        when(principal.getPrincipal()).thenReturn(token);
        assertThatThrownBy(() -> controller.getTokenAttributes(principal))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unknown principal token");

        assertThatThrownBy(() -> controller.getTokenAttributes(mock(AbstractAuthenticationToken.class)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unknown principal");
    }

    @Test
    void testGetOrganization() {
        try (val utilities = Mockito.mockStatic(SecurityContextHolder.class)) {
            val securityContext = mock(SecurityContext.class);
            val principal = mock(AbstractAuthenticationToken.class);
            val oAuth2User = mock(DefaultOAuth2User.class);
            when(principal.getPrincipal()).thenReturn(oAuth2User);
            when(oAuth2User.getAttributes()).thenReturn(Map.of("key", "value"));
            when(securityContext.getAuthentication()).thenReturn(principal);
            utilities.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            val controller = new BaseController();
            assertThatThrownBy(controller::getOrganization)
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Token details do not contain organization id");
        }
    }
}