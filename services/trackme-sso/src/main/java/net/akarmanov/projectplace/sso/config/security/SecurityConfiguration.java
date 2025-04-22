package net.akarmanov.projectplace.sso.config.security;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.akarmanov.projectplace.sso.config.security.handler.CustomAuthenticationSuccessHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Slf4j
@EnableWebSecurity
@RequiredArgsConstructor
@EnableConfigurationProperties(AuthorizationServerProperties.class)
@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

  public static final String LOGIN_PAGE = "/client/login";

  static final String[] PERMIT_ALL_PATTERNS = {
      LOGIN_PAGE,
      "/registration-success",
      "/static/**",
      "/client/**",
      "/v3/api-docs",
      "/api/v1/registration/**",
      "/v3/api-docs/swagger-config"
  };

  private final UserDetailsService userDetailService;

  private final PasswordEncoder passwordEncoder;

  private final AuthorizationServerProperties properties;

  // handlers
  private AuthenticationSuccessHandler loginRequestSuccessHandler;

  private AuthenticationFailureHandler failureHandler;

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

    http.csrf(AbstractHttpConfigurer::disable);

    http.exceptionHandling(configurer ->
        configurer.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));

    return http.formLogin(formLogin ->
            formLogin
                .loginPage(LOGIN_PAGE)
                .loginProcessingUrl(LOGIN_PAGE))
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

  @PostConstruct
  private void initializeHandlers() {
    // создаём кастомный AuthenticationSuccessHandler для формы логина
    this.loginRequestSuccessHandler = new CustomAuthenticationSuccessHandler(
        properties.getAuthenticationSuccessUrl(),
        properties.getCustomHandlerHeaderName()
    );

    // указываем стандартный AuthenticationSuccessHandler для OAuth2 Client
    this.failureHandler = new SimpleUrlAuthenticationFailureHandler();
  }
}
