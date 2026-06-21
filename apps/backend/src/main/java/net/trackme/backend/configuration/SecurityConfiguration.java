package net.trackme.backend.configuration;

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

import lombok.RequiredArgsConstructor;
import static net.trackme.backend.models.UserRole.ADMIN;
import static net.trackme.backend.models.UserRole.SUPER_ADMIN;
import static net.trackme.backend.models.UserRole.TRACKER;

/**
 * Настройки безопасности.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    /**
     * Устанавливает фильтры безопасности.
     *
     * @param http объект {@link HttpSecurity}
     * @return объект {@link SecurityFilterChain}
     * @throws Exception возможное исключение
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(request -> request
                        // Публичные эндпоинты без аутентификации
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-resources/*",
                                "/actuator/**",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml/**",
                                "/api/v1/users/register").permitAll()
                        
                        // Admin эндпоинты требуют ADMIN/SUPER_ADMIN роль
                        .requestMatchers("/api/v1/admin/team-cards/by-user")
                                .hasAnyRole(ADMIN.toString(), SUPER_ADMIN.toString())
                        .requestMatchers("/api/v1/admin/team-cards/reassign")
                                .hasAnyRole(ADMIN.toString(), SUPER_ADMIN.toString())
                        .requestMatchers("/api/v1/admin/**")
                                .hasAnyRole(ADMIN.toString(), SUPER_ADMIN.toString())
                        
                        // Обычные tracker эндпоинты
                        .requestMatchers("/api/v1/**")
                                .hasRole(TRACKER.toString())
                        
                        // Super admin эндпоинты
                        .requestMatchers("/api/v1/super-admin/**")
                                .hasRole(SUPER_ADMIN.toString())
                        
                        // Всё остальное требует аутентификации
                        .anyRequest().authenticated())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(rs -> rs.jwt(jwt -> jwt
                        .jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    /**
     * Конструирует {@link RoleHierarchy}.
     *
     * @return иерархия ролей
     */
    @Bean
    public RoleHierarchy roleHierarchy() {
        String hierarchy = "ROLE_SUPER_ADMIN > ROLE_ADMIN\n"
                         + "ROLE_ADMIN > ROLE_TRACKER";
        return RoleHierarchyImpl.fromHierarchy(hierarchy);
    }

    /**
     * Создает конвертер для JWT аутентификации.
     *
     * @return конвертер JWT аутентификации
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter =
                new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");

        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtConverter;
    }
}
