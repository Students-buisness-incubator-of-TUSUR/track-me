package net.trackme.sso.services.impl;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2DeviceCode;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2UserCode;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.util.Assert;

import java.time.Duration;

public final class RedisOAuth2AuthorizationService implements OAuth2AuthorizationService {

  /**
   * Префикс ключа для объектов OAuth2Authorization, для которых уже существует токен доступа.
   * Т.е. являющихся завершёнными
   */
  private static final String COMPLETE_KEY_PREFIX = "oauth2_authorization_complete:";

  /**
   * Префикс ключа для объектов OAuth2Authorization, для которых процесс авторизации ещё не завершился
   * и токен доступа ещё нет. Данная ситуация может возникнуть при authorization code flow,
   * на этапе работы с authorization code. До запроса на получение токенов доступа.
   */
  private static final String INIT_KEY_PREFIX = "oauth2_authorization_init:";

  private final RedisTemplate<String, OAuth2Authorization> redisTemplate;

  private final ValueOperations<String, OAuth2Authorization> valueOperations;

  private final Duration timeToLive;

  public RedisOAuth2AuthorizationService(RedisTemplate<String, OAuth2Authorization> redisTemplate,
                                         Duration timeToLive) {
    this.redisTemplate = redisTemplate;
    this.valueOperations = redisTemplate.opsForValue();
    this.timeToLive = timeToLive;
  }

  private static boolean isCompleted(OAuth2Authorization authorization) {
    return authorization.getAccessToken() != null;
  }

  private static boolean hasToken(
      OAuth2Authorization authorization,
      String token,
      @Nullable OAuth2TokenType tokenType
  ) {
    if (tokenType == null) {
      return matchesState(authorization, token) ||
             matchesAuthorizationCode(authorization, token) ||
             matchesAccessToken(authorization, token) ||
             matchesIdToken(authorization, token) ||
             matchesRefreshToken(authorization, token) ||
             matchesDeviceCode(authorization, token) ||
             matchesUserCode(authorization, token);
    } else if (OAuth2ParameterNames.STATE.equals(tokenType.getValue())) {
      return matchesState(authorization, token);
    } else if (OAuth2ParameterNames.CODE.equals(tokenType.getValue())) {
      return matchesAuthorizationCode(authorization, token);
    } else if (OAuth2TokenType.ACCESS_TOKEN.equals(tokenType)) {
      return matchesAccessToken(authorization, token);
    } else if (OidcParameterNames.ID_TOKEN.equals(tokenType.getValue())) {
      return matchesIdToken(authorization, token);
    } else if (OAuth2TokenType.REFRESH_TOKEN.equals(tokenType)) {
      return matchesRefreshToken(authorization, token);
    } else if (OAuth2ParameterNames.DEVICE_CODE.equals(tokenType.getValue())) {
      return matchesDeviceCode(authorization, token);
    } else if (OAuth2ParameterNames.USER_CODE.equals(tokenType.getValue())) {
      return matchesUserCode(authorization, token);
    }
    return false;
  }

  private static boolean matchesState(OAuth2Authorization authorization, String token) {
    return token.equals(authorization.getAttribute(OAuth2ParameterNames.STATE));
  }

  private static boolean matchesAuthorizationCode(OAuth2Authorization authorization, String token) {
    OAuth2Authorization.Token<OAuth2AuthorizationCode> authorizationCode =
        authorization.getToken(OAuth2AuthorizationCode.class);
    return authorizationCode != null && authorizationCode.getToken().getTokenValue().equals(token);
  }

  private static boolean matchesAccessToken(OAuth2Authorization authorization, String token) {
    OAuth2Authorization.Token<OAuth2AccessToken> accessToken =
        authorization.getToken(OAuth2AccessToken.class);
    return accessToken != null && accessToken.getToken().getTokenValue().equals(token);
  }

  private static boolean matchesRefreshToken(OAuth2Authorization authorization, String token) {
    OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken =
        authorization.getToken(OAuth2RefreshToken.class);
    return refreshToken != null && refreshToken.getToken().getTokenValue().equals(token);
  }

  private static boolean matchesIdToken(OAuth2Authorization authorization, String token) {
    OAuth2Authorization.Token<OidcIdToken> idToken =
        authorization.getToken(OidcIdToken.class);
    return idToken != null && idToken.getToken().getTokenValue().equals(token);
  }

  private static boolean matchesDeviceCode(OAuth2Authorization authorization, String token) {
    OAuth2Authorization.Token<OAuth2DeviceCode> deviceCode =
        authorization.getToken(OAuth2DeviceCode.class);
    return deviceCode != null && deviceCode.getToken().getTokenValue().equals(token);
  }

  private static boolean matchesUserCode(OAuth2Authorization authorization, String token) {
    OAuth2Authorization.Token<OAuth2UserCode> userCode =
        authorization.getToken(OAuth2UserCode.class);
    return userCode != null && userCode.getToken().getTokenValue().equals(token);
  }

  @Override
  public void save(OAuth2Authorization authorization) {
    Assert.notNull(authorization, "authorization must not be null");
    String key;
    if (isCompleted(authorization)) {
      key = COMPLETE_KEY_PREFIX + authorization.getId();

      var initKey = INIT_KEY_PREFIX + authorization.getId();
      if (Boolean.TRUE.equals(this.redisTemplate.hasKey(initKey))) {
        redisTemplate.delete(initKey);
      }
    } else {
      key = INIT_KEY_PREFIX + authorization.getId();
    }
    this.valueOperations.set(key, authorization, timeToLive);
  }

  @Override
  public void remove(OAuth2Authorization authorization) {
    Assert.notNull(authorization, "authorization must not be null");
    String key;
    if (isCompleted(authorization)) {
      key = COMPLETE_KEY_PREFIX + authorization.getId();
    } else {
      key = INIT_KEY_PREFIX + authorization.getId();
    }
    this.redisTemplate.delete(key);
  }

  @Override
  public OAuth2Authorization findById(String id) {
    Assert.hasText(id, "id must not be null or empty");
    var authorization = this.valueOperations.get(COMPLETE_KEY_PREFIX + id);
    return authorization != null
        ? authorization
        : this.valueOperations.get(INIT_KEY_PREFIX + id);
  }

  @Override
  public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
    Assert.hasText(token, "token must not be null or empty");
    var authorization = this.findByToken(token, tokenType, COMPLETE_KEY_PREFIX);
    return authorization != null
        ? authorization
        : this.findByToken(token, tokenType, INIT_KEY_PREFIX);
  }

  private OAuth2Authorization findByToken(String token,
                                          OAuth2TokenType tokenType,
                                          String completeKeyPrefix) {
    var keys = redisTemplate.keys(completeKeyPrefix + "*");
    for (String key : keys) {
      var authorization = this.valueOperations.get(key);
      if (hasToken(authorization, token, tokenType)) {
        return authorization;
      }
    }
    return null;
  }
}
