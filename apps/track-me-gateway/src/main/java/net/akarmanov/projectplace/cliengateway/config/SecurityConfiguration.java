package net.akarmanov.projectplace.cliengateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {
  @Bean
  SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
    http
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .cors(corsSpec -> corsSpec.configurationSource(request -> {
          var corsConfiguration = new CorsConfiguration();
          corsConfiguration.setAllowedOriginPatterns(List.of("*"));
          corsConfiguration.setAllowedMethods(List.of("GET",
              "POST",
              "PUT",
              "DELETE",
              "OPTIONS",
              "PATCH"));
          corsConfiguration.setAllowedHeaders(List.of("*"));
          corsConfiguration.setAllowCredentials(true);
          return corsConfiguration;
        }))
        .authorizeExchange(exchange -> exchange
            .anyExchange().permitAll())
        .oauth2Client(Customizer.withDefaults());
    return http.build();
  }
}
