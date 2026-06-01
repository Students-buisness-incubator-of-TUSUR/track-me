package net.trackme.cliengateway.config;
import java.net.URI;
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
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;

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
                .oauth2Login(oauth2Login -> {
                        oauth2Login.authorizationRequestResolver(
                                new CustomAuthorizationRequestResolver(clientRegistrationRepository));
                        oauth2Login.authenticationSuccessHandler(authenticationSuccessHandler);
                })
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
        ReactiveClientRegistrationRepository repo = registrationId ->
                this.clientRegistrationRepository.findByRegistrationId(registrationId)
                        .map(reg -> {
                            var metadata = new HashMap<>(reg.getProviderDetails().getConfigurationMetadata());
                            metadata.put("end_session_endpoint", appProperties.logoutUri());
                            return ClientRegistration.withClientRegistration(reg)
                                    .providerConfigurationMetadata(metadata)
                                    .build();
                        });

        var serverLogoutSuccessHandler = new OidcClientInitiatedServerLogoutSuccessHandler(repo);
        serverLogoutSuccessHandler.setPostLogoutRedirectUri(appProperties.afterLogoutUri());
        this.logoutSuccessHandler = serverLogoutSuccessHandler;

        this.authenticationSuccessHandler = (webFilterExchange, authentication) -> {
            var exchange = webFilterExchange.getExchange();
            return exchange.getSession()
                    .flatMap(session -> {
                        var redirectUri = (String) session.getAttributes()
                                .remove(CustomAuthorizationRequestResolver.SESSION_KEY);
                        var target = (redirectUri != null) ? redirectUri : appProperties.afterLoginUrl();
                        String baseTarget = (authentication.getPrincipal() instanceof OAuth2User)
                                ? "http://localhost:9000/client/registration" 
                                : ((redirectUri != null) ? redirectUri : appProperties.afterLoginUrl());
                        String finalTargetUrl = baseTarget;
                        if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
                            String email = "";
                            String name = "";
                            // Проверка на Google (по ключу sub)
                            if (oauth2User.getAttributes().containsKey("sub")) {
                                email = oauth2User.getAttribute("email");
                                name = oauth2User.getAttribute("name");
                            } 
                            // Проверка на Яндекс. У него в качестве user-name-attribute мы указали id
                            else if (oauth2User.getAttributes().containsKey("id")) {
                                // === ЛОГИКА ДЛЯ ЯНДЕКСА ===
                                // Яндекс может вернуть email в default_email
                                email = oauth2User.getAttribute("default_email");
                                if (email == null) {
                                    email = oauth2User.getAttribute("email");
                                }
                                // Имя пользователя
                                name = oauth2User.getAttribute("real_name");
                                if (name == null) {
                                    name = oauth2User.getAttribute("display_name");
                                }
                            }
                            if (email == null) email = "";
                            if (name == null) name = "";

                            finalTargetUrl = UriComponentsBuilder.fromUriString(baseTarget)
                                    .queryParam("email", email)
                                    .queryParam("name", name)
                                    .encode() 
                                    .build()
                                    .toUriString();

                        }
                        
                        return new RedirectServerAuthenticationSuccessHandler(finalTargetUrl)
                                .onAuthenticationSuccess(webFilterExchange, authentication);
                    });
        };
    }
}