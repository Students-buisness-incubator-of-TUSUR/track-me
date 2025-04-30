package net.akarmanov.projectplace.cliengateway.config;

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
    http
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .authorizeExchange(exchange ->
            exchange.pathMatchers(OPTIONS, "/**").permitAll()
                .pathMatchers("/actuator/**").permitAll()
                .anyExchange().authenticated())
        .oauth2Login(oauth2Login ->
            oauth2Login.authenticationSuccessHandler(authenticationSuccessHandler))
        .oauth2Client(withDefaults())
        .logout(logout -> logout
            .logoutSuccessHandler(logoutSuccessHandler));
    return http.build();
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
        new OidcClientInitiatedServerLogoutSuccessHandler(this.clientRegistrationRepository);
    serverLogoutSuccessHandler.setPostLogoutRedirectUri(appProperties.getAfterLogoutUri());
    this.logoutSuccessHandler = serverLogoutSuccessHandler;

    this.authenticationSuccessHandler = new RedirectServerAuthenticationSuccessHandler(
        appProperties.getAfterLoginUrl()
    );
  }
}
