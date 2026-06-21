package net.trackme.sso.config.security;

import net.trackme.sso.services.impl.RedisOAuth2AuthorizationConsentService;
import net.trackme.sso.services.impl.RedisOAuth2AuthorizationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;

import java.time.Duration;

@Configuration
public class SecurityConfigurationUtility {
  @Bean
  OAuth2AuthorizationService redisOAuth2AuthorizationService(
      RedisTemplate<String, OAuth2Authorization> redisTemplate) {
    return new RedisOAuth2AuthorizationService(redisTemplate,
        Duration.ofHours(1));
  }

  @Bean
  public OAuth2AuthorizationConsentService oAuth2AuthorizationConsentService(
      RedisTemplate<String, OAuth2AuthorizationConsent> redisTemplate
  ) {
    return new RedisOAuth2AuthorizationConsentService(
        redisTemplate,
        Duration.ofHours(1)
    );
  }
}
