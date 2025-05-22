package net.akarmanov.projectplace.trackmereportservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth ->
                        auth
                                .requestMatchers("/api/v1/report/excel").authenticated()
                                .requestMatchers(
                                        "/swagger-ui/**",
                                        "/swagger-resources/*",
                                        "/actuator/**",
                                        "/v3/api-docs/**",
                                        "/v3/api-docs.yaml/**",
                                        "/v3/api-docs.yaml").permitAll()
                                .anyRequest().permitAll());
        return http.build();
    }
}
