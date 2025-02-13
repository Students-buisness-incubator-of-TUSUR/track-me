package net.akarmanov.projectplace.services.jwt;

import org.springframework.security.core.userdetails.UserDetails;

/**
 * Сервис для работы с JWT.
 */
public interface JwtService {
  /**
   * Генерирует токен для пользователя.
   *
   * @param username Имя пользователя.
   * @return Токен.
   */
  String generateToken(UserDetails userDetails);

  /**
   * Возвращает имя пользователя из токена.
   *
   * @param token Токен.
   * @return Имя пользователя.
   */
  String getUsernameFromToken(String token);

  /**
   * Производит проверку токена.
   *
   * @param token Токен.
   * @return Результат проверки.
   */
  boolean isTokenValid(String token, UserDetails userDetails);

  /**
   * Проверяет, истек ли срок действия токена.
   *
   * @param token Токен.
   * @return Результат проверки.
   */
  boolean isTokenExpired(String token);
}
