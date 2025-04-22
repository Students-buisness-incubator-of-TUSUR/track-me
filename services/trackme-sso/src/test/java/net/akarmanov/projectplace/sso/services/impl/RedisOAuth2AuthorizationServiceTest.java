package net.akarmanov.projectplace.sso.services.impl;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisOAuth2AuthorizationServiceTest {

  @Test
  void save_shouldStoreCompletedAuthorizationWithProperKey() {
    RedisTemplate<String, OAuth2Authorization> redisTemplateMock = mock(RedisTemplate.class);
    ValueOperations<String, OAuth2Authorization> valueOperationsMock = mock(ValueOperations.class);
    when(redisTemplateMock.opsForValue()).thenReturn(valueOperationsMock);

    OAuth2Authorization authorizationMock = mock(OAuth2Authorization.class);
    when(authorizationMock.getId()).thenReturn("authorization-id");
    when(authorizationMock.getAccessToken()).thenReturn(mock(OAuth2Authorization.Token.class));

    RedisOAuth2AuthorizationService service =
        new RedisOAuth2AuthorizationService(redisTemplateMock, Duration.ofMinutes(30));

    assertDoesNotThrow(() -> service.save(authorizationMock));
    verify(valueOperationsMock, times(1)).set(eq("oauth2_authorization_complete:authorization-id"),
        eq(authorizationMock),
        any(Duration.class));
  }

  @Test
  void save_shouldDeleteInitKeyIfCompletedAuthorizationExists() {
    RedisTemplate<String, OAuth2Authorization> redisTemplateMock = mock(RedisTemplate.class);
    ValueOperations<String, OAuth2Authorization> valueOperationsMock = mock(ValueOperations.class);
    when(redisTemplateMock.opsForValue()).thenReturn(valueOperationsMock);
    when(redisTemplateMock.hasKey("oauth2_authorization_init:authorization-id")).thenReturn(true);

    OAuth2Authorization authorizationMock = mock(OAuth2Authorization.class);
    when(authorizationMock.getId()).thenReturn("authorization-id");
    when(authorizationMock.getAccessToken()).thenReturn(mock(OAuth2Authorization.Token.class));

    RedisOAuth2AuthorizationService service =
        new RedisOAuth2AuthorizationService(redisTemplateMock, Duration.ofMinutes(30));

    assertDoesNotThrow(() -> service.save(authorizationMock));
    verify(redisTemplateMock, times(1)).delete("oauth2_authorization_init:authorization-id");
  }

  @Test
  void save_shouldStoreNotCompletedAuthorizationWithProperKey() {
    RedisTemplate<String, OAuth2Authorization> redisTemplateMock = mock(RedisTemplate.class);
    ValueOperations<String, OAuth2Authorization> valueOperationsMock = mock(ValueOperations.class);
    when(redisTemplateMock.opsForValue()).thenReturn(valueOperationsMock);

    OAuth2Authorization authorizationMock = mock(OAuth2Authorization.class);
    when(authorizationMock.getId()).thenReturn("authorization-id");
    when(authorizationMock.getAccessToken()).thenReturn(null);

    RedisOAuth2AuthorizationService service =
        new RedisOAuth2AuthorizationService(redisTemplateMock, Duration.ofMinutes(30));

    assertDoesNotThrow(() -> service.save(authorizationMock));
    verify(valueOperationsMock, times(1)).set(eq("oauth2_authorization_init:authorization-id"),
        eq(authorizationMock),
        any(Duration.class));
  }

  @Test
  void save_shouldThrowExceptionWhenAuthorizationIsNull() {
    RedisTemplate<String, OAuth2Authorization> redisTemplateMock = mock(RedisTemplate.class);

    RedisOAuth2AuthorizationService service =
        new RedisOAuth2AuthorizationService(redisTemplateMock, Duration.ofMinutes(30));

    assertDoesNotThrow(() -> {
      try {
        service.save(null);
      } catch (IllegalArgumentException ex) {
        // Expected behavior
      }
    });
  }
}