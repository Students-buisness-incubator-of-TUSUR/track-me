package net.trackme.cliengateway.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

class YandexAuthorizationResolverTest {
    @Test
    void yandexButtonStartsTrackmeOidcWithProviderHint() {
        var client = ClientRegistration.withRegistrationId("track-me-client")
                .clientId("track-me-client").clientSecret("test")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://sso.example.org/oauth2/authorize")
                .tokenUri("https://sso.example.org/oauth2/token").scope("openid", "profile").build();
        var resolver = new CustomAuthorizationRequestResolver(new InMemoryReactiveClientRegistrationRepository(client), true);
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get(
                "https://api.example.org/oauth2/authorization/yandex?redirect_uri=https://web.example.org/after-login"));
        var request = resolver.resolve(exchange).block();
        assertNotNull(request);
        assertEquals("https://sso.example.org/oauth2/authorize", request.getAuthorizationUri());
        assertEquals("yandex", request.getAdditionalParameters().get("login_hint"));
        assertEquals("track-me-client", request.getAttribute("registration_id"));
        assertTrue(request.getRedirectUri().endsWith("/login/oauth2/code/track-me-client"));
        assertNotNull(request.getState());
        assertEquals("https://web.example.org/after-login", exchange.getSession().block().getAttribute(CustomAuthorizationRequestResolver.SESSION_KEY));
    }
}
