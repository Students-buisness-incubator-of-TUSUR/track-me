package net.akarmanov.projectplace.authservice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;

import javax.sql.DataSource;

@Configuration
@EnableConfigurationProperties(AuthorizationServerProperties.class)
public class AuthorizationServerConfiguration {

  @Bean
  UserDetailsManager userDetailsManager(DataSource dataSource, PasswordEncoder passwordEncoder) {
    var detailsManager = new JdbcUserDetailsManager(dataSource);
    detailsManager.createUser(User.builder()
        .username("superadmin")
        .password("superadmin")
        .roles("SUPERADMIN")
        .passwordEncoder(passwordEncoder::encode)
        .build());
    return detailsManager;
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
