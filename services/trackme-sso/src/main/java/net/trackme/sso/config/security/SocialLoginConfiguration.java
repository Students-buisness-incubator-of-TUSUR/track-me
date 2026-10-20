package net.trackme.sso.config.security;

import net.trackme.sso.services.social.SocialLoginHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
@org.springframework.boot.autoconfigure.condition.ConditionalOnExpression("${app.yandex.enabled:false} or ${app.google.enabled:false}")
public class SocialLoginConfiguration {
    @Bean
    ClientRegistrationRepository socialClientRegistrations(
            @Value("${app.yandex.enabled:false}") boolean yandexEnabled,
            @Value("${app.google.enabled:false}") boolean googleEnabled,
            @Value("${YANDEX_CLIENT_ID:}") String yandexId, @Value("${YANDEX_CLIENT_SECRET:}") String yandexSecret,
            @Value("${GOOGLE_CLIENT_ID:}") String googleId, @Value("${GOOGLE_CLIENT_SECRET:}") String googleSecret,
            @Value("${spring.security.oauth2.authorizationserver.issuer}") String issuer,
            @Value("${YANDEX_REDIRECT_URI:}") String yandexCallback,
            @Value("${GOOGLE_REDIRECT_URI:}") String googleCallback,
            @Value("${GOOGLE_PHONE_ENABLED:false}") boolean phoneEnabled) {
        var registrations = new java.util.ArrayList<ClientRegistration>();
        if (yandexEnabled) registrations.add(yandexClientRegistrations(yandexId, yandexSecret, issuer, yandexCallback)
                .findByRegistrationId("yandex"));
        if (googleEnabled) {
            var scopes = new java.util.ArrayList<>(java.util.List.of("openid", "email", "profile"));
            if (phoneEnabled) scopes.add(net.trackme.sso.services.social.GooglePhoneService.PHONE_SCOPE);
            registrations.add(org.springframework.security.config.oauth2.client.CommonOAuth2Provider.GOOGLE
                    .getBuilder("google").clientId(googleId).clientSecret(googleSecret)
                    .redirectUri(googleCallback.isBlank()
                            ? org.springframework.util.StringUtils.trimTrailingCharacter(issuer, '/') + "/login/oauth2/code/google"
                            : googleCallback).scope(scopes).build());
        }
        return new InMemoryClientRegistrationRepository(registrations);
    }

    ClientRegistrationRepository yandexClientRegistrations(
            @Value("${YANDEX_CLIENT_ID}") String clientId,
            @Value("${YANDEX_CLIENT_SECRET}") String clientSecret,
            @Value("${spring.security.oauth2.authorizationserver.issuer}") String issuer,
            @Value("${YANDEX_REDIRECT_URI:}") String redirectUri) {
        return new InMemoryClientRegistrationRepository(ClientRegistration.withRegistrationId("yandex")
                .clientId(clientId).clientSecret(clientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(redirectUri.isBlank()
                        ? org.springframework.util.StringUtils.trimTrailingCharacter(issuer, '/') + "/login/oauth2/code/yandex" : redirectUri)
                .authorizationUri("https://oauth.yandex.ru/authorize")
                .tokenUri("https://oauth.yandex.ru/token")
                .userInfoUri("https://login.yandex.ru/info?format=json")
                .userNameAttributeName("id").scope("login:info", "login:email")
                .clientName("Яндекс").build());
    }

    @Bean
    @Order(3)
    SecurityFilterChain socialLoginChain(HttpSecurity http, SocialLoginHandler handler,
            @Value("${FRONTEND_URI:http://localhost}") String frontendUri) throws Exception {
        return http.securityMatcher("/oauth2/authorization/yandex", "/login/oauth2/code/yandex",
                        "/oauth2/authorization/google", "/login/oauth2/code/google", "/client/yandex/**", "/client/social/**")
                .cors(cors -> cors.configurationSource(request -> {
                    var configuration = new org.springframework.web.cors.CorsConfiguration();
                    configuration.setAllowedOrigins(java.util.List.of(frontendOrigin(frontendUri)));
                    configuration.setAllowedMethods(java.util.List.of("GET", "POST", "OPTIONS"));
                    configuration.setAllowedHeaders(java.util.List.of("Content-Type", "X-CSRF-TOKEN"));
                    configuration.setAllowCredentials(true);
                    return configuration;
                }))
                .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
                .oauth2Login(login -> login.loginPage(SecurityConfiguration.LOGIN_PAGE)
                        .userInfoEndpoint(info -> info.oidcUserService(request -> {
                            var user = new org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService().loadUser(request);
                            var attributes = new java.util.HashMap<String, Object>(user.getClaims());
                            attributes.put("trackme_phone", net.trackme.sso.services.social.GooglePhoneService.create()
                                    .find(request.getAccessToken().getTokenValue(), request.getAccessToken().getScopes()));
                            return new org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser(user.getAuthorities(),
                                    user.getIdToken(), new org.springframework.security.oauth2.core.oidc.OidcUserInfo(attributes), "sub");
                        }))
                        .successHandler(handler).failureHandler((request, response, exception) -> {
                            request.getSession().removeAttribute(SocialLoginHandler.PENDING);
                            response.sendRedirect(request.getContextPath() + "/client/yandex/complete?error");
                        }))
                .build();
    }
    static String frontendOrigin(String value) {
        var uri = java.net.URI.create(value);
        var builder = org.springframework.web.util.UriComponentsBuilder.fromUri(uri).replacePath(null)
                .replaceQuery(null).fragment(null);
        if (("http".equals(uri.getScheme()) && uri.getPort() == 80)
                || ("https".equals(uri.getScheme()) && uri.getPort() == 443)) builder.port(-1);
        return builder.build().toUriString();
    }
}
