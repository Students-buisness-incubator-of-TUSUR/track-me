package net.akarmanov.projectplace.authservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;

import javax.sql.DataSource;

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
    return detailsManager;
  }
}
