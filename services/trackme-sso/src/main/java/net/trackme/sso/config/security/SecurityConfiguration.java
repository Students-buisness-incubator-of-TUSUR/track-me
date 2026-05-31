package net.trackme.sso.config.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

import static org.springframework.http.HttpMethod.OPTIONS;
import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Конфигурация безопасности для SSO-сервиса.
 * Настраивает цепочку фильтров безопасности, роли и права доступа.
 */
@Slf4j
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

    /** Страница входа в систему. */
    public static final String LOGIN_PAGE = "/client/login";

    /** Шаблоны URL, доступные без аутентификации. */
    static final String[] PERMIT_ALL_PATTERNS = {
            LOGIN_PAGE,
            "/registration-success",
            "/static/**",
            "/client/**",
            "/actuator/**",
            "/v3/api-docs",
            "/api/csrf",
            "/api/v1/registration/**",
            "/v3/api-docs/swagger-config",
            "/.well-known/**"
    };

    /** Сервис для загрузки данных пользователя. */
    private final UserDetailsService userDetailService;

    /** Кодировщик паролей. */
    private final PasswordEncoder passwordEncoder;

    /**
     * Создаёт обработчик выражений безопасности с учётом иерархии ролей.
     *
     * @param roleHierarchy иерархия ролей
     * @return настроенный обработчик выражений
     */
    @Bean
    static MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            RoleHierarchy roleHierarchy) {
        DefaultMethodSecurityExpressionHandler handler =
                new DefaultMethodSecurityExpressionHandler();
        handler.setRoleHierarchy(roleHierarchy);
        return handler;
    }

    /**
     * Настраивает цепочку фильтров безопасности.
     *
     * @param http конфигурация HTTP-безопасности
     * @return настроенная цепочка фильтров
     * @throws Exception если произошла ошибка конфигурации
     */
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
                        new HttpStatusEntryPoint(
                                HttpStatus.UNAUTHORIZED)));

        return http.formLogin(formLogin ->
                        formLogin
                                .loginPage(LOGIN_PAGE)
                                .loginProcessingUrl(LOGIN_PAGE))
                .build();
    }
}
