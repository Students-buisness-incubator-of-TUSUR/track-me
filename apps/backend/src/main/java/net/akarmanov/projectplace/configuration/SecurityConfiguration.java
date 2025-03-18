package net.akarmanov.projectplace.configuration;

import lombok.RequiredArgsConstructor;
import net.akarmanov.projectplace.rest.filters.JwtAuthenticationFilter;
import net.akarmanov.projectplace.services.user.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

import static net.akarmanov.projectplace.models.UserRole.ADMIN;
import static net.akarmanov.projectplace.models.UserRole.SUPER_ADMIN;

/**
 * Настройки безопасности.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {
  private final UserService userService;

  private final PasswordEncoder passwordEncoder;

  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  private final AuthenticationEntryPoint authenticationEntryPoint;

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
        .cors(cors -> cors.configurationSource(request -> {
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
        .authorizeHttpRequests(request -> request
            .requestMatchers("/api/v1/auth/**").permitAll()
            .requestMatchers(
                "/swagger-ui/**",
                "/swagger-resources/*",
                "/v3/api-docs/**",
                "/v3/api-docs.yaml/**",
                "/v3/api-docs.yaml").permitAll()
            .requestMatchers("/api/v1/admin/**").hasRole(ADMIN.toString())
            .requestMatchers("/api/v1/super-admin/**").hasRole(SUPER_ADMIN.toString())
            .anyRequest().authenticated())
        .sessionManagement(manager -> manager.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .exceptionHandling(handler -> handler.authenticationEntryPoint(authenticationEntryPoint));
    return http.build();
  }
  /**
   * Конструирует {@link AuthenticationManager}.
   *
   * @return {@link AuthenticationManager}.
   */
  @Bean
  public AuthenticationManager authenticationManager() {
    DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
    authenticationProvider.setUserDetailsService(userService);
    authenticationProvider.setPasswordEncoder(passwordEncoder);

    ProviderManager providerManager = new ProviderManager(authenticationProvider);
    providerManager.setEraseCredentialsAfterAuthentication(false);

    return providerManager;
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

  /**
   * Конструирует {@link SuperAdminSetupConfigurer}.
   *
   * @return {@link SuperAdminSetupConfigurer}.
   */
  @Bean
  public SuperAdminSetupConfigurer superAdminSetupConfigurer() {
    return new SuperAdminSetupConfigurer(userService, passwordEncoder);
  }
}
