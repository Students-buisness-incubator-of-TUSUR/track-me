package net.trackme.sso.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class OAuth2ResourceServerConfiguration {
  @Bean
  @Order(2)
  public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
    http
        .securityMatcher("/api/**")
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers("/api/v1/account/**").hasRole("TRACKER")
            .requestMatchers("/api/v1/users/**").hasRole("TRACKER")
            .requestMatchers("/api/v1/registration/**").permitAll()
            .requestMatchers("/api/csrf").permitAll()
            .anyRequest().authenticated()
        )
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
        );
    return http.build();
  }

  @Bean
  public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter =
        new JwtGrantedAuthoritiesConverter();
    grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_"); // Добавляем префикс "ROLE_"
    grantedAuthoritiesConverter.setAuthoritiesClaimName("roles"); // Читаем роли из "roles"

    JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
    jwtConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
    return jwtConverter;
  }

  /**
   * Конструирует {@link RoleHierarchy}.
   *
   * @return Иерархия ролей.
   */
  @Bean
  public RoleHierarchy roleHierarchy() {
    String hierarchy = "ROLE_SUPER_ADMIN > ROLE_ADMIN\n" +
                       "ROLE_ADMIN > ROLE_TRACKER";
    return RoleHierarchyImpl.fromHierarchy(hierarchy);
  }

}
