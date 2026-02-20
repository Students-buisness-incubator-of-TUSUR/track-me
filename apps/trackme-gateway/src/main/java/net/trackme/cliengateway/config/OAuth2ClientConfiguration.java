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
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

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
        return http
                .cors(withDefaults()) // Enable CORS support
                .authorizeExchange(exchange ->
                        exchange.pathMatchers(OPTIONS, "/**").permitAll()
                                .pathMatchers("/actuator/**").permitAll()
                                .pathMatchers("/csrf").permitAll()
                                .anyExchange().authenticated())
                .oauth2Login(oauth2Login ->
                        oauth2Login.authenticationSuccessHandler(authenticationSuccessHandler))
                .oauth2Client(withDefaults())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(logoutSuccessHandler))
                .build();
    }

    @Bean
    public CorsWebFilter corsWebFilter() {
        var corsProperties = appProperties.cors();
        var configuration = new CorsConfiguration();

        // Set allowed origins or patterns (patterns are more flexible for development)
        boolean hasOrigins = corsProperties.allowedOrigins() != null
                            && !corsProperties.allowedOrigins().isEmpty();
        boolean hasPatterns = corsProperties.allowedOriginPatterns() != null
                             && !corsProperties.allowedOriginPatterns().isEmpty();

        if (!hasOrigins && !hasPatterns) {
            throw new IllegalStateException(
                "CORS configuration error: either allowedOrigins or "
                + "allowedOriginPatterns must be specified");
        }

        if (hasOrigins) {
            configuration.setAllowedOrigins(corsProperties.allowedOrigins());
        }
        if (hasPatterns) {
            configuration.setAllowedOriginPatterns(corsProperties.allowedOriginPatterns());
        }

        configuration.setAllowedMethods(corsProperties.allowedMethods());
        configuration.setAllowedHeaders(corsProperties.allowedHeaders());

        // Set exposed headers if configured
        if (corsProperties.exposedHeaders() != null
            && !corsProperties.exposedHeaders().isEmpty()) {
            configuration.setExposedHeaders(corsProperties.exposedHeaders());
        }

        configuration.setAllowCredentials(corsProperties.allowCredentials());
        configuration.setMaxAge(3600L);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return new CorsWebFilter(source);
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

            var redirectUri = exchange.getRequest().getQueryParams().getFirst("redirect_uri");
            if (redirectUri != null) {
                return new RedirectServerAuthenticationSuccessHandler(redirectUri)
                        .onAuthenticationSuccess(webFilterExchange, authentication);
            }
            return new RedirectServerAuthenticationSuccessHandler(appProperties.afterLoginUrl())
                    .onAuthenticationSuccess(webFilterExchange, authentication);
        };
    }
}