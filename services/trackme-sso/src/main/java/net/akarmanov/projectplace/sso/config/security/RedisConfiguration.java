package net.akarmanov.projectplace.sso.config.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;

@Slf4j
@Configuration
public class RedisConfiguration {

  @Bean
  RedisTemplate<String, OAuth2Authorization> authorizationRedisTemplate(
      RedisConnectionFactory redisConnectionFactory) {
    RedisTemplate<String, OAuth2Authorization> redisTemplate = new RedisTemplate<>();
    redisTemplate.setConnectionFactory(redisConnectionFactory);
    redisTemplate.setKeySerializer(new StringRedisSerializer());
    return redisTemplate;
  }

  @Bean
  RedisTemplate<String, OAuth2AuthorizationConsent> authorizationConsentRedisTemplate(
      RedisConnectionFactory redisConnectionFactory) {
    RedisTemplate<String, OAuth2AuthorizationConsent> redisTemplate = new RedisTemplate<>();
    redisTemplate.setConnectionFactory(redisConnectionFactory);
    redisTemplate.setKeySerializer(new StringRedisSerializer());
    return redisTemplate;
  }
}
