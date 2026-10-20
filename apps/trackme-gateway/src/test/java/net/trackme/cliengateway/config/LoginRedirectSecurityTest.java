package net.trackme.cliengateway.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.server.WebSessionServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.config.EnableWebFlux;

@SpringJUnitConfig(LoginRedirectSecurityTest.Config.class)
@org.springframework.test.context.TestPropertySource(properties = {
        "app.after-login-url=https://trackme.example/after-login",
        "app.after-logout-uri=https://trackme.example",
        "app.logout-uri=https://sso.example/connect/logout",
        "app.sso-registration-url=https://sso.example/client/registration",
        "app.cors.allowed-origins=https://trackme.example",
        "app.cors.allowed-methods=GET,POST,OPTIONS",
        "app.cors.allowed-headers=Content-Type,X-CSRF-TOKEN",
        "app.cors.allow-credentials=true"
})
class LoginRedirectSecurityTest {
    @Autowired ApplicationContext context;
    private WebTestClient client;

    @BeforeEach
    void setUp() { client = WebTestClient.bindToApplicationContext(context).build(); }

    @Test
    void anonymousLoginRendersInsteadOfRedirectingToItself() {
        client.get().uri("/login").exchange().expectStatus().isOk()
                .expectHeader().doesNotExist("Location")
                .expectBody(String.class).value(body -> org.junit.jupiter.api.Assertions.assertTrue(body.contains("Google")));
    }

    @Test
    void failedLoginRemainsAReachableErrorPage() {
        client.get().uri("/login?error").exchange().expectStatus().isOk()
                .expectHeader().doesNotExist("Location");
    }

    @Test
    void protectedPageStillRequiresAuthentication() {
        client.get().uri("/backend/private").header("Accept", "text/html").exchange()
                .expectStatus().is3xxRedirection().expectHeader().valueEquals("Location", "/login");
    }

    @Test
    void expiredSessionApiReturns401WithoutRedirect() {
        client.get().uri("/session/status").exchange().expectStatus().isUnauthorized()
                .expectHeader().doesNotExist("Location");
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebFlux
    @EnableWebFluxSecurity
    @Import(OAuth2ClientConfiguration.class)
    static class Config {
        @Bean
        ReactiveClientRegistrationRepository registrations() {
            return new InMemoryReactiveClientRegistrationRepository(registration("google", "Google"),
                    registration("track-me-client", "TrackMe SSO"));
        }

        private ClientRegistration registration(String id, String name) {
            return ClientRegistration.withRegistrationId(id).clientId("test-client").clientSecret("test-secret")
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                    .authorizationUri("https://provider.example/authorize").tokenUri("https://provider.example/token")
                    .userInfoUri("https://provider.example/userinfo").userNameAttributeName("id").clientName(name).build();
        }

        @Bean
        ServerOAuth2AuthorizedClientRepository authorizedClients() {
            return new WebSessionServerOAuth2AuthorizedClientRepository();
        }
    }
}
