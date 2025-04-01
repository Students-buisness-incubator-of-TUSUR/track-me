package net.akarmanov.projectplace.authservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.Set;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toSet;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(AuthorizationServerProperties.class)
public class AuthServerConfiguration {

  private final AuthorizationServerProperties properties;

  @Bean
  UserDetailsManager userDetailsManager(DataSource dataSource) {
    var jdbcProperties = properties.getJdbc();
    var detailsManager = new JdbcUserDetailsManager(dataSource);
    detailsManager.setUsersByUsernameQuery(jdbcProperties.getSelectUserSql());
    detailsManager.setAuthoritiesByUsernameQuery(jdbcProperties.getRoleSql());
    detailsManager.setAuthoritiesByUsernameQuery(jdbcProperties.getAuthoritiesByUsernameSql());
    return detailsManager;
  }

  @Bean
  OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer() {
    return (context) -> {
      if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
        context.getClaims().claims((claims) -> {
          Set<String> roles =
              AuthorityUtils.authorityListToSet(context.getPrincipal().getAuthorities())
                  .stream()
                  .map(c -> c.replaceFirst("^ROLE_", ""))
                  .collect(collectingAndThen(toSet(), Collections::unmodifiableSet));
          claims.put("roles", roles);
        });
      }
    };
  }
}
