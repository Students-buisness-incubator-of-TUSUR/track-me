package net.akarmanov.projectplace.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestClient;

/**
 * Настройки приложения.
 */
@Configuration
@EnableConfigurationProperties({AppProperties.class, AclAppProperties.class})
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class AppConfiguration {

  @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
  private String issuerUri;

  /**
   * Парольный кодировщик. Используется BCrypt шифрование.
   *
   * @return Парольный кодировщик.
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  RestClient asRestClient(RestClient.Builder restClientBuilder) {
    return restClientBuilder
        .baseUrl(issuerUri)
        .build();
  }
}
