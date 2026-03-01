package com.authbox.server.service;

import com.authbox.base.dao.OauthClientDao;
import com.authbox.base.dao.OauthTokenDao;
import com.authbox.base.dao.OauthUserDao;
import com.authbox.base.exception.BadRequestException;
import com.authbox.base.exception.Oauth2Exception;
import com.authbox.base.model.OauthClient;
import com.authbox.base.model.OauthToken;
import com.authbox.base.model.OauthUser;
import com.authbox.base.model.Organization;
import com.authbox.base.model.TokenFormat;
import com.authbox.base.model.TokenType;
import com.authbox.base.service.AccessLogService;
import com.authbox.base.util.CertificateKeysUtils;
import lombok.val;
import org.apache.commons.beanutils.BeanUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.InvocationTargetException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static com.authbox.base.util.HashUtils.sha256;
import static com.authbox.base.util.IdUtils.createId;
import static com.authbox.server.TestUtils.assertLogEntryContainsAndReset;
import static com.authbox.server.filter.RequestWrapperFilter.REQUEST_START_REQUEST_TIME_MDC_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TokenDetailsServiceTest {

    private OauthClientDao oauthClientDao;
    private OauthUserDao oauthUserDao;
    private AccessLogService accessLogService;
    private OauthTokenDao oauthTokenDao;
    private TokenDetailsService service;

    @BeforeEach
    public void setup() {
        oauthClientDao = mock(OauthClientDao.class);
        oauthUserDao = mock(OauthUserDao.class);
        oauthTokenDao = mock(OauthTokenDao.class);
        accessLogService = mock(AccessLogService.class);
        val defaultClock = Clock.systemUTC();
        service = new TokenDetailsServiceImpl(
                oauthTokenDao,
                oauthUserDao,
                oauthClientDao,
                defaultClock,
                new ObjectMapper(),
                accessLogService
        );
        MDC.put(REQUEST_START_REQUEST_TIME_MDC_KEY, System.currentTimeMillis() + "");
    }

    @AfterEach
    public void teardown() {
        MDC.clear();
    }

    @Test
    public void testGetAccessTokenDetails() throws InvocationTargetException, IllegalAccessException {
        val organization = Organization.builder()
                .withId(createId())
                .withCreateTime(Instant.now())
                .withName("Some Organization")
                .withDomainPrefix("some.domain")
                .withEnabled(true)
                .build();
        var accessToken = new AtomicReference<>(createId());
        var accessTokenHash = sha256(accessToken.get());
        assertThatThrownBy(() -> service.getAccessTokenDetails(organization, accessToken.get(), null))
                .isInstanceOf(Oauth2Exception.class)
                .hasMessageContaining("unauthorized request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Validating Oauth2 access token details",
                "Unable to find Oauth2 token by hash='%s'".formatted(accessTokenHash)
        );

        val oauthToken = OauthToken.builder()
                .withId(createId())
                .withCreateTime(Instant.now())
                .withScopes(List.of("some/scope"))
                .withHash(accessTokenHash)
                .withClientId("clientId")
                .withOauthUserId("userId")
                .withOrganizationId("orgId")
                .withTokenType(TokenType.AUTHORIZATION_CODE)
                .build();
        when(oauthTokenDao.getByHash(accessTokenHash)).thenReturn(Optional.of(oauthToken));
        assertThatThrownBy(() -> service.getAccessTokenDetails(organization, accessToken.get(), null))
                .isInstanceOf(Oauth2Exception.class)
                .hasMessageContaining("unauthorized request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Validating Oauth2 access token details",
                "auth2 token is not ACCESS_TOKEN type. hash='%s'".formatted(accessTokenHash)
        );

        oauthToken.setTokenType(TokenType.ACCESS_TOKEN);
        assertThatThrownBy(() -> service.getAccessTokenDetails(organization, accessToken.get(), null))
                .isInstanceOf(Oauth2Exception.class)
                .hasMessageContaining("unauthorized request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Validating Oauth2 access token details",
                "Oauth2 token organization id='orgId' does not match Oauth2 client specified organization id='%s'"
                        .formatted(organization.getId())
        );

        oauthToken.setOrganizationId(organization.getId());
        assertThatThrownBy(() -> service.getAccessTokenDetails(organization, accessToken.get(), null))
                .isInstanceOf(Oauth2Exception.class)
                .hasMessageContaining("unauthorized request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Validating Oauth2 access token details",
                "Oauth2 token validated",
                "Unable to find Oauth2 client by client id='clientId'"
        );

        val oauthClient = OauthClient.builder()
                .withId(createId())
                .withCreateTime(Instant.now())
                .withSecret("secret")
                .withEnabled(false)
                .withScopes(List.of())
                .build();
        when(oauthClientDao.getById(oauthClient.getId())).thenReturn(Optional.of(oauthClient));
        oauthToken.setClientId(oauthClient.getId());
        assertThatThrownBy(() -> service.getAccessTokenDetails(organization, accessToken.get(), null))
                .isInstanceOf(Oauth2Exception.class)
                .hasMessageContaining("unauthorized request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Validating Oauth2 access token details",
                "Oauth2 token validated",
                "Oauth2 client is disabled. client id='%s'".formatted(oauthClient.getId())
        );

        oauthToken.setClientId(oauthClient.getId());
        oauthClient.setEnabled(true);
        assertThatThrownBy(() -> service.getAccessTokenDetails(organization, accessToken.get(), null))
                .isInstanceOf(Oauth2Exception.class)
                .hasMessageContaining("unauthorized request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Validating Oauth2 access token details",
                "Oauth2 token validated",
                "Oauth2 client organization id='null' does not match Oauth2 client specified organization id='%s'"
                        .formatted(organization.getId())
        );


        OauthClient oauthClient2 = OauthClient.builder().build();
        BeanUtils.copyProperties(oauthClient2, oauthClient);
        oauthClient2.setId("bad-client-id");
        assertThatThrownBy(() -> service.getAccessTokenDetails(organization, accessToken.get(), oauthClient2))
                .isInstanceOf(Oauth2Exception.class)
                .hasMessageContaining("unauthorized request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Validating Oauth2 access token details",
                "Oauth2 token validated",
                ("Oauth2 client provided (client id: 'bad-client-id') does not correspond to Oauth2 client associated " +
                        "with provided token (client id: '%s')").formatted(oauthClient.getId())
        );

        oauthClient.setOrganizationId(organization.getId());
        oauthClient.setExpiration(Duration.ofSeconds(60));
        oauthToken.setExpiration(Instant.now().minusSeconds(10));
        assertThat(
                service.getAccessTokenDetails(organization, accessToken.get(), null)
        ).isNotEmpty().containsEntry("active", false);
        assertLogEntryContainsAndReset(
                accessLogService,
                "Validating Oauth2 access token details",
                "Oauth2 token validated"
        );

        oauthToken.setExpiration(Instant.now().plusSeconds(10));
        assertThatThrownBy(() -> service.getAccessTokenDetails(organization, accessToken.get(), null))
                .isInstanceOf(Oauth2Exception.class)
                .hasMessageContaining("unauthorized request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Validating Oauth2 access token details",
                "Oauth2 token validated",
                "Oauth2 user not found by id='userId'"
        );

        val oauthUser = OauthUser.builder()
                .withId(createId())
                .withCreateTime(Instant.now())
                .withUsername("username")
                .withPassword("password")
                .withEnabled(false)
                .withOrganizationId(organization.getId())
                .build();
        oauthToken.setOauthUserId(oauthUser.getId());
        when(oauthUserDao.getById(oauthUser.getId())).thenReturn(Optional.of(oauthUser));
        assertThatThrownBy(() -> service.getAccessTokenDetails(organization, accessToken.get(), null))
                .isInstanceOf(Oauth2Exception.class)
                .hasMessageContaining("unauthorized request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Validating Oauth2 access token details",
                "Oauth2 token validated",
                "OauthUser user disabled id='%s'".formatted(oauthUser.getId())
        );

        oauthUser.setEnabled(true);
        assertThat(
                service.getAccessTokenDetails(organization, accessToken.get(), null)
        ).isNotEmpty()
                .containsKey("expires")
                .containsKey("expires_in")
                .containsEntry("active", true)
                .containsEntry("client_id", oauthClient.getId())
                .containsEntry("organization_id", organization.getId())
                .containsEntry("scope", "some/scope")
                .containsEntry("user_id", oauthUser.getId())
                .containsEntry("username", oauthUser.getUsername());
        assertLogEntryContainsAndReset(
                accessLogService,
                "Validating Oauth2 access token details",
                "Oauth2 token validated"
        );

        oauthClient.setTokenFormat(TokenFormat.JWT);
        accessToken.set("eyJhbGciOiJSUzM4NCJ9.eyJpc3MiOiIzQUVaMGprVTZhSVlKVzg4c3pvU1hnSW1OclgiLCJzdWIiOiIzQUVaMGVCNVdNVlgxc3lqMGhGVWJHSXZwYkwiLCJzY29wZSI6InNvbWUvc2NvcGUiLCJvcmdhbml6YXRpb25faWQiOiIzQUVaMGprVTZhSVlKVzg4c3pvU1hnSW1OclgiLCJpYXQiOjE3NzIxNTk0NjgsImV4cCI6MTc3MjE2MDA2OCwidXNlcl9pZCI6IjNBRVowZmhOdmdNRVVRYXN4ZlFUWmlpN1M1SCIsIm1ldGFkYXRhIjp7fX0.grWS224EDe91_JsIWsnioQfoB24N8v52D5gQg-1BYHLzf3qcedbJE_sXZ3rM-NZuUf4Vi9TqVAlf_NsHN_0cuqIpOquQH6vNK-IHrradJsksISGOC_v-xPfDR5pNOYqivWyuoiBv0ClMVhkUCRVUZ2BQi_1jyu9DyCsntfy1wD_dD6Lek6yVorlFHxaEJiip5I7wHZETJOUcIBHJaA6OcFwNsrRjDe9nuPQaJ64zZLtSynV_Ttuv-RRkvIatfqjf_JI9kTNoSXbXJhnX4qPkT0mJ6j0wUw23xCyvf1cSdiUV1FirKbvXP2kHfgS_DCC-EKcIUhTUawE_K9Z_BwmUOA");
        accessTokenHash = sha256(accessToken.get());
        when(oauthTokenDao.getByHash(accessTokenHash)).thenReturn(Optional.of(oauthToken));
        assertThatThrownBy(() -> service.getAccessTokenDetails(organization, accessToken.get(), null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid request");
        assertLogEntryContainsAndReset(
                accessLogService,
                "Validating Oauth2 access token details",
                "Oauth2 token validated",
                "Oauth2 client with id='%s' does not have public key to validate JWT token.".formatted(oauthClient.getId())
        );

        val rsaKeyPair = CertificateKeysUtils.generateRsaKeyPair();
        oauthClient.setPrivateKey(rsaKeyPair.privateKeyPem);
        oauthClient.setPublicKey(rsaKeyPair.publicKeyPem);
        assertThatThrownBy(() -> service.getAccessTokenDetails(organization, accessToken.get(), null))
                .isInstanceOf(Oauth2Exception.class)
                .hasMessageContaining("unauthorized request");
    }
}