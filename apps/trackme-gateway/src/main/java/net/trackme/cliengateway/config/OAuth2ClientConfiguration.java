package net.trackme.cliengateway.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.oidc.web.server.logout.OidcClientInitiatedServerLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;

import static org.springframework.http.HttpMethod.OPTIONS;
import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
@EnableConfigurationProperties({AppProperties.class})
public class OAuth2ClientConfiguration {
    private final ReactiveClientRegistrationRepository clientRegistrationRepository;

    private final AppProperties appProperties;

    private ServerLogoutSuccessHandler logoutSuccessHandler;

    private ServerAuthenticationSuccessHandler authenticationSuccessHandler;

    @Bean
    SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        var corsConfig = appProperties.cors();
        return http
                .cors(corsSpec -> corsSpec.configurationSource(exchange -> {
                    var config = new CorsConfiguration();
                    config.setAllowedOrigins(corsConfig.allowedOrigins());
                    config.setAllowedMethods(corsConfig.allowedMethods());
                    config.setAllowedHeaders(corsConfig.allowedHeaders());
                    config.setAllowCredentials(corsConfig.allowCredentials());
                    return config;
                }))
                .authorizeExchange(exchange ->
                        exchange.pathMatchers(OPTIONS, "/**").permitAll()
                                .pathMatchers("/actuator/**").permitAll()
                                .pathMatchers("/csrf").permitAll()
                                .anyExchange().authenticated())
                .oauth2Login(oauth2Login ->
                        oauth2Login.authenticationSuccessHandler(authenticationSuccessHandler))
                .oauth2Client(withDefaults())
                .logout(logout -> logout
                        .logoutSuccessHandler(logoutSuccessHandler))
                .build();
    }

    @Bean
    ReactiveOAuth2AuthorizedClientManager authorizedClientManager(
            ReactiveClientRegistrationRepository clientRegistrationRepository,
            ServerOAuth2AuthorizedClientRepository clientRepository) {
        var authorizedClientProvider = ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
                .authorizationCode()
                .refreshToken()
                .build();
        var authorizedClientManager =
                new DefaultReactiveOAuth2AuthorizedClientManager
                        (clientRegistrationRepository, clientRepository);
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
        return authorizedClientManager;
    }

    @PostConstruct
    private void initializeHandlers() {
        var serverLogoutSuccessHandler =
                new OidcClientInitiatedServerLogoutSuccessHandler(
                        this.clientRegistrationRepository);
        serverLogoutSuccessHandler.setPostLogoutRedirectUri(appProperties.afterLogoutUri());
        this.logoutSuccessHandler = serverLogoutSuccessHandler;

        this.authenticationSuccessHandler = (webFilterExchange, authentication) -> {
            var exchange = webFilterExchange.getExchange();

            // Получаем redirect_uri из параметров
            String redirectUri = exchange.getRequest().getQueryParams().getFirst("redirect_uri");
            if (redirectUri != null) {
                return new RedirectServerAuthenticationSuccessHandler(redirectUri)
                        .onAuthenticationSuccess(webFilterExchange, authentication);
            }
            // Фолбэк на старое поведение
            return new RedirectServerAuthenticationSuccessHandler(appProperties.afterLoginUrl())
                    .onAuthenticationSuccess(webFilterExchange, authentication);
        };
    }
}
