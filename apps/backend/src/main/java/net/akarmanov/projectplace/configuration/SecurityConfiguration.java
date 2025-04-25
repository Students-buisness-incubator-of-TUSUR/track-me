package net.akarmanov.projectplace.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import static net.akarmanov.projectplace.models.UserRole.ADMIN;
import static net.akarmanov.projectplace.models.UserRole.SUPER_ADMIN;
import static net.akarmanov.projectplace.models.UserRole.TRACKER;

/**
 * Настройки безопасности.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

  /**
   * Устонавливает фильтры безопасности.
   *
   * @param http объект {@link HttpSecurity}
   * @return объект {@link SecurityFilterChain}
   * @throws Exception возможное исключение.
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .cors(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(request -> request
            .requestMatchers("/api/v1/auth/**").permitAll()
            .requestMatchers(
                "/swagger-ui/**",
                "/swagger-resources/*",
                "/actuator/**",
                "/v3/api-docs/**",
                "/v3/api-docs.yaml/**",
                "/api/v1/users/register",
                "/v3/api-docs.yaml").permitAll()
            .requestMatchers("/api/v1/admin/**").hasRole(ADMIN.toString())
            .requestMatchers("/api/v1/**").hasRole(TRACKER.toString())
            .requestMatchers("/api/v1/super-admin/**").hasRole(SUPER_ADMIN.toString())
            .anyRequest().authenticated())
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .oauth2ResourceServer(rs ->
            rs.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
    return http.build();
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
}
