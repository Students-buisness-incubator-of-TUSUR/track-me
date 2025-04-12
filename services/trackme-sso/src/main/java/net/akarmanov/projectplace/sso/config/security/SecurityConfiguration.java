package net.akarmanov.projectplace.sso.config.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

@Slf4j
@EnableWebSecurity
@RequiredArgsConstructor
@EnableConfigurationProperties(AuthorizationServerProperties.class)
@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

  public static final String LOGIN_PAGE = "/login";

  static final String[] PERMIT_ALL_PATTERNS = {
      LOGIN_PAGE,
      "/static/**",
      "/v3/api-docs",
      "/swagger-ui/**",
      "/api/v1/registration/register",
      "/v3/api-docs/swagger-config",
      "/api/v1/registration/confirm",
      "/index"
  };

  private final UserDetailsService userDetailService;

  private final PasswordEncoder passwordEncoder;

  // handlers

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http)
      throws Exception {
    http
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers(PERMIT_ALL_PATTERNS).permitAll()
            .anyRequest().authenticated());

    http.getSharedObject(AuthenticationManagerBuilder.class)
        .userDetailsService(userDetailService)
        .passwordEncoder(passwordEncoder);
    return http.formLogin(withDefaults())
        .build();
  }

  @Bean
  FilterRegistrationBean<CorsFilter> corsFilter() {
    log.info("CREATING CORS FILTER");
    var corsConfig = new CorsConfiguration();
    corsConfig.setAllowedOrigins(List.of(CorsConfiguration.ALL)); // или "*" для тестов
    corsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    corsConfig.setAllowedHeaders(List.of("*"));

    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", corsConfig);
    return new FilterRegistrationBean<>(new CorsFilter(source));
  }
}
