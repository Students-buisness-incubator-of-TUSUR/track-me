package net.trackme.cliengateway.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.oidc.web.server.logout.OidcClientInitiatedServerLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

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
                .cors(withDefaults())
                .authorizeExchange(exchange ->
                        exchange.pathMatchers(OPTIONS, "/**").permitAll()
                                .pathMatchers("/actuator/**").permitAll()
                                .pathMatchers("/csrf").permitAll()
                                .anyExchange().authenticated())
                .exceptionHandling(exceptions ->
                        exceptions.authenticationEntryPoint(ajaxAwareEntryPoint()))
                .oauth2Login(oauth2Login ->
                        oauth2Login.authenticationSuccessHandler(authenticationSuccessHandler))
                .oauth2Client(withDefaults())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(logoutSuccessHandler))
                .build();
    }

    /**
     * Для AJAX-запросов (XHR / fetch с Accept: application/json) возвращаем 401
     * с заголовком X-Login-Url, чтобы frontend мог сделать полный редирект.
     * Для обычных браузерных запросов — стандартный 302 на OAuth2 login.
     */
    private ServerAuthenticationEntryPoint ajaxAwareEntryPoint() {
        var loginUrl = "/oauth2/authorization/track-me-client";

        return (exchange, ex) -> {
            var request = exchange.getRequest();
            var accept = request.getHeaders().getAccept();
            var xRequestedWith = request.getHeaders().getFirst("X-Requested-With");

            boolean isAjax = "XMLHttpRequest".equals(xRequestedWith)
                    || accept.stream().anyMatch(mt ->
                    mt.isCompatibleWith(MediaType.APPLICATION_JSON)
                            && !mt.isCompatibleWith(MediaType.TEXT_HTML));

            if (isAjax) {
                var response = exchange.getResponse();
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                response.getHeaders().set("X-Login-Url", loginUrl);
                response.getHeaders().set(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "X-Login-Url");
                return response.setComplete();
            }

            // Обычный запрос — редирект на OAuth2
            return exchange.getResponse().setComplete().then(Mono.fromRunnable(() -> {
                var response = exchange.getResponse();
                response.setStatusCode(HttpStatus.FOUND);
                response.getHeaders().setLocation(
                        java.net.URI.create(loginUrl));
            }));
        };
    }

    @Bean
    public CorsWebFilter corsWebFilter() {
        var corsProperties = appProperties.cors();
        var configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.allowedOrigins());
        configuration.setAllowedMethods(corsProperties.allowedMethods());
        configuration.setAllowedHeaders(corsProperties.allowedHeaders());
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