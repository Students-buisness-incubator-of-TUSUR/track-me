package net.akarmanov.projectplace.sso.config.security;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.akarmanov.projectplace.sso.config.security.handler.CustomAuthenticationSuccessHandler;
import net.akarmanov.projectplace.sso.services.CustomOAuth2UserService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
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
      "/api/v1/registration/confirm"
  };

  private final CustomOAuth2UserService oAuth2UserService;

  private final UserDetailsService userDetailService;

  private final PasswordEncoder passwordEncoder;

  private final AuthorizationServerProperties authorizationServerProperties;

  // handlers
  private AuthenticationSuccessHandler oAuth2successHandler;

  private AuthenticationSuccessHandler loginRequestSuccessHandler;

  private AuthenticationFailureHandler failureHandler;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http)
      throws Exception {
    var socialConfigurer = new SocialConfigurer()
        .oAuth2UserService(oAuth2UserService)
        .successHandler(oAuth2successHandler)
        .failureHandler(failureHandler)
        .formLogin(LOGIN_PAGE);

    http
        .with(socialConfigurer, withDefaults())
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers(PERMIT_ALL_PATTERNS).permitAll()
            .anyRequest().authenticated());

    http.getSharedObject(AuthenticationManagerBuilder.class)
        .userDetailsService(userDetailService)
        .passwordEncoder(passwordEncoder);
    http
        .csrf(AbstractHttpConfigurer::disable);
    return http.formLogin(formLogin ->
            formLogin.successHandler(loginRequestSuccessHandler)
                .failureHandler(failureHandler))
        .build();
  }

  @PostConstruct
  private void initializeHandlers() {
    // создаём кастомный AuthenticationSuccessHandler для формы логина
    this.loginRequestSuccessHandler = new CustomAuthenticationSuccessHandler(
        authorizationServerProperties.getAuthenticationSuccessUrl(),
        authorizationServerProperties.getCustomHandlerHeaderName()
    );

    // указываем стандартный AuthenticationSuccessHandler для OAuth2 Client
    var handler = new SavedRequestAwareAuthenticationSuccessHandler();
    handler.setDefaultTargetUrl(authorizationServerProperties.getAuthenticationSuccessUrl());
    this.oAuth2successHandler = handler;

    this.failureHandler = new SimpleUrlAuthenticationFailureHandler();
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
