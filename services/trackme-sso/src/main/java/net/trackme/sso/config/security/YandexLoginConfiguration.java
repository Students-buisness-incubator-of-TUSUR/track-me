package net.trackme.sso.config.security;

import net.trackme.sso.services.yandex.YandexLoginHandler;
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
@ConditionalOnProperty(name = "app.yandex.enabled", havingValue = "true")
public class YandexLoginConfiguration {
    @Bean
    ClientRegistrationRepository yandexClientRegistrations(
            @Value("${YANDEX_CLIENT_ID}") String clientId,
            @Value("${YANDEX_CLIENT_SECRET}") String clientSecret,
            @Value("${spring.security.oauth2.authorizationserver.issuer}") String issuer) {
        return new InMemoryClientRegistrationRepository(ClientRegistration.withRegistrationId("yandex")
                .clientId(clientId).clientSecret(clientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(issuer.replaceAll("/+$", "") + "/login/oauth2/code/yandex")
                .authorizationUri("https://oauth.yandex.ru/authorize")
                .tokenUri("https://oauth.yandex.ru/token")
                .userInfoUri("https://login.yandex.ru/info?format=json")
                .userNameAttributeName("id").scope("login:info", "login:email")
                .clientName("Яндекс").build());
    }

    @Bean
    @Order(3)
    SecurityFilterChain yandexLoginChain(HttpSecurity http, YandexLoginHandler handler) throws Exception {
        return http.securityMatcher("/oauth2/authorization/yandex", "/login/oauth2/code/yandex", "/client/yandex/**")
                .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
                .oauth2Login(login -> login.loginPage(SecurityConfiguration.LOGIN_PAGE)
                        .successHandler(handler).failureHandler((request, response, exception) -> {
                            request.getSession().removeAttribute(YandexLoginHandler.PENDING);
                            response.sendRedirect(request.getContextPath() + "/client/yandex/complete?error");
                        }))
                .build();
    }
}
