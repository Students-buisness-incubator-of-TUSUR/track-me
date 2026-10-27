package net.trackme.sso.services.impl;

import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcLogoutAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcLogoutAuthenticationToken;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LogoutAfterTokenRefreshTest {
    private final Map<String, OAuth2Authorization> storage = new HashMap<>();
    private final RegisteredClient client = RegisteredClient.withId("client")
            .clientId("track-me-client").authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("https://app.example/callback").postLogoutRedirectUri("https://app.example")
            .build();
    private RedisOAuth2AuthorizationService service;
    private OidcLogoutAuthenticationProvider provider;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        RedisTemplate<String, OAuth2Authorization> redis = mock(RedisTemplate.class);
        ValueOperations<String, OAuth2Authorization> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(anyString())).thenAnswer(call -> storage.get(call.getArgument(0)));
        doAnswer(call -> {
            storage.put(call.getArgument(0), call.getArgument(1));
            return null;
        }).when(values).set(anyString(), any(OAuth2Authorization.class), any(Duration.class));
        when(redis.keys(anyString())).thenAnswer(call -> {
            String prefix = call.<String>getArgument(0).replace("*", "");
            return storage.keySet().stream().filter(key -> key.startsWith(prefix))
                    .collect(java.util.stream.Collectors.toSet());
        });
        when(redis.delete(anyString())).thenAnswer(call -> storage.remove(call.getArgument(0)) != null);
        service = new RedisOAuth2AuthorizationService(redis, Duration.ofHours(2));
        provider = new OidcLogoutAuthenticationProvider(new InMemoryRegisteredClientRepository(client),
                service, new SessionRegistryImpl());
    }

    @Test
    void acceptsOriginalExpiredIdTokenAfterMultipleRefreshes() {
        OAuth2Authorization authorization = saveAndRefresh();
        var request = request("original-id-token");

        var result = (OidcLogoutAuthenticationToken) provider.authenticate(request);

        assertTrue(result.isAuthenticated());
        assertEquals("original-id-token", result.getIdToken().getTokenValue());
        assertEquals("latest-id-token", service.findById(authorization.getId()).getToken(OidcIdToken.class)
                .getToken().getTokenValue());
        assertNull(service.findByToken("old-access-token", OAuth2TokenType.ACCESS_TOKEN));
    }

    @Test
    void acceptsLatestIdTokenAsWell() {
        saveAndRefresh();
        assertTrue(provider.authenticate(request("latest-id-token")).isAuthenticated());
    }

    @Test
    void rejectsUnknownTokenAndUnregisteredRedirect() {
        saveAndRefresh();
        var unknown = request("unknown-token");
        var redirect = new OidcLogoutAuthenticationToken("original-id-token", principal(), null,
                null, "https://untrusted.example", null);
        assertThrows(OAuth2AuthenticationException.class, () -> provider.authenticate(unknown));
        assertThrows(OAuth2AuthenticationException.class, () -> provider.authenticate(redirect));
    }

    @Test
    void rejectsOriginalTokenAfterAuthorizationRemoved() {
        service.remove(saveAndRefresh());
        var request = request("original-id-token");
        assertThrows(OAuth2AuthenticationException.class, () -> provider.authenticate(request));
    }

    @Test
    void rejectsOriginalTokenWhenCurrentIdTokenInvalidated() {
        var authorization = saveAndRefresh();
        var idToken = authorization.getToken(OidcIdToken.class).getToken();
        service.save(OAuth2Authorization.from(authorization).token(idToken,
                metadata -> metadata.put(OAuth2Authorization.Token.INVALIDATED_METADATA_NAME, true)).build());
        var request = request("original-id-token");
        assertThrows(OAuth2AuthenticationException.class, () -> provider.authenticate(request));
    }

    @Test
    void migratesAuthorizationStoredBeforeInitialTokenRetentionWasAdded() {
        Instant now = Instant.now();
        var legacy = originalAuthorization(now);
        storage.put("oauth2_authorization_complete:" + legacy.getId(), legacy);

        service.save(OAuth2Authorization.from(legacy)
                .token(idToken("latest-id-token", now, now.plusSeconds(900))).build());

        assertTrue(provider.authenticate(request("original-id-token")).isAuthenticated());
    }

    @Test
    void initialIdTokenDoesNotRestoreOldAccessTokenOrReplaceCurrentOne() {
        var authorization = saveAndRefresh();

        var byAccessToken = service.findByToken("current-access-token", OAuth2TokenType.ACCESS_TOKEN);
        var byInitialIdToken = service.findByToken("original-id-token", null);

        assertEquals(authorization.getId(), byAccessToken.getId());
        assertEquals("latest-id-token", byAccessToken.getToken(OidcIdToken.class).getToken().getTokenValue());
        assertEquals("original-id-token", byInitialIdToken.getToken(OidcIdToken.class).getToken().getTokenValue());
        assertNull(service.findByToken("original-id-token", OAuth2TokenType.ACCESS_TOKEN));
        assertNull(service.findByToken("old-access-token", null));
    }

    @Test
    void nonOidcAuthorizationStillResolvesByAccessToken() {
        Instant now = Instant.now();
        var authorization = OAuth2Authorization.withRegisteredClient(client).id("non-oidc")
                .principalName("tracker").authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .accessToken(new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
                        "non-oidc-access", now, now.plusSeconds(900))).build();
        service.save(authorization);

        assertSame(authorization, service.findByToken("non-oidc-access", null));
    }

    private OAuth2Authorization saveAndRefresh() {
        Instant now = Instant.now();
        var original = originalAuthorization(now);
        service.save(original);
        for (String value : List.of("refreshed-id-token", "latest-id-token")) {
            var current = service.findById(original.getId());
            service.save(OAuth2Authorization.from(current)
                    .accessToken(new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
                            "current-access-token", now, now.plusSeconds(900)))
                    .token(idToken(value, now, now.plusSeconds(900))).build());
        }
        return service.findById(original.getId());
    }

    private OAuth2Authorization originalAuthorization(Instant now) {
        return OAuth2Authorization.withRegisteredClient(client).id("authorization")
                .principalName("tracker").authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .attribute(Principal.class.getName(), principal())
                .accessToken(new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "old-access-token",
                        now.minusSeconds(3600), now.minusSeconds(1800)))
                .token(idToken("original-id-token", now.minusSeconds(3600), now.minusSeconds(1800)))
                .build();
    }

    private OidcIdToken idToken(String value, Instant issued, Instant expires) {
        return new OidcIdToken(value, issued, expires,
                Map.of("sub", "tracker", "aud", java.util.List.of(client.getClientId())));
    }

    private OidcLogoutAuthenticationToken request(String hint) {
        return new OidcLogoutAuthenticationToken(hint, principal(), null, null, "https://app.example", null);
    }

    private UsernamePasswordAuthenticationToken principal() {
        return UsernamePasswordAuthenticationToken.authenticated("tracker", null, java.util.List.of());
    }
}
