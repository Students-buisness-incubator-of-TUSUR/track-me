package net.trackme.sso.config.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

import static org.springframework.http.HttpMethod.OPTIONS;
import static org.springframework.security.config.Customizer.withDefaults;

@Slf4j
@EnableWebSecurity
@RequiredArgsConstructor
@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

    public static final String LOGIN_PAGE = "/client/login";

    static final String[] PERMIT_ALL_PATTERNS = {
            LOGIN_PAGE,
            "/registration-success",
            "/static/**",
            "/client/**",
            "/actuator/**",
            "/v3/api-docs",
            "/api/csrf",
            "/api/v1/registration/**",
            "/v3/api-docs/swagger-config"
    };

    private final UserDetailsService userDetailService;

    private final PasswordEncoder passwordEncoder;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(PERMIT_ALL_PATTERNS).permitAll()
                        .requestMatchers(OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated());

        http.getSharedObject(AuthenticationManagerBuilder.class)
                .userDetailsService(userDetailService)
                .passwordEncoder(passwordEncoder);

        http.csrf(withDefaults());
        http.cors(AbstractHttpConfigurer::disable);

        http.exceptionHandling(configurer ->
                configurer.authenticationEntryPoint(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));

        return http.formLogin(formLogin ->
                        formLogin
                                .loginPage(LOGIN_PAGE)
                                .loginProcessingUrl(LOGIN_PAGE))
                .build();
    }
}
