package net.akarmanov.projectplace.cliengateway.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.oidc.web.server.logout.OidcClientInitiatedServerLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
import org.springframework.security.web.server.csrf.ServerCsrfTokenRequestAttributeHandler;

import static org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository.withHttpOnlyFalse;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class OAuth2ClientConfiguration {
  private final ReactiveClientRegistrationRepository clientRegistrationRepository;

  private ServerLogoutSuccessHandler logoutSuccessHandler;

  @Bean
  SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
    http
        .csrf(csrf -> csrf.csrfTokenRepository(withHttpOnlyFalse())
            .csrfTokenRequestHandler(new ServerCsrfTokenRequestAttributeHandler()))
        .authorizeExchange(exchange ->
            exchange.anyExchange().authenticated())
        .oauth2Login(Customizer.withDefaults())
        .oauth2Client(Customizer.withDefaults())
        .logout(logout -> logout
            .logoutSuccessHandler(logoutSuccessHandler));
    return http.build();
  }

  @PostConstruct
  private void initializeHandlers() {
    var serverLogoutSuccessHandler =
        new OidcClientInitiatedServerLogoutSuccessHandler(this.clientRegistrationRepository);
    serverLogoutSuccessHandler.setPostLogoutRedirectUri("{baseUrl}");
    this.logoutSuccessHandler = serverLogoutSuccessHandler;
  }
}
