package net.akarmanov.projectplace.sso.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;

@Configuration
public class SecurityConfigurationUtility {
  @Bean
  public OAuth2AuthorizationService oAuth2AuthorizationService() {
    return new InMemoryOAuth2AuthorizationService();
  }
}
