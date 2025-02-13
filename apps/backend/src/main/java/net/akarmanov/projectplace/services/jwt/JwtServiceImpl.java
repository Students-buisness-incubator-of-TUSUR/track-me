package net.akarmanov.projectplace.services.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import net.akarmanov.projectplace.domain.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Реализация сервиса для работы с JWT.
 */
@Service
@RequiredArgsConstructor
class JwtServiceImpl implements JwtService {

  private final SecretKey secretKey;

  @Override
  public String generateToken(UserDetails userDetails) {
    Map<String, Object> claims = new HashMap<>();
    if (userDetails instanceof User user) {
      claims.put("id", user.getId());
      claims.put("role", user.getRole().name());
      claims.put("telegramId", user.getTelegramId());
    }
    return generateToken(claims, userDetails);
  }

  @Override
  public String getUsernameFromToken(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  @Override
  public boolean isTokenValid(String token, UserDetails userDetails) {
    var username = getUsernameFromToken(token);
    return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
  }

  @Override
  public boolean isTokenExpired(String token) {
    return extractAllClaims(token).getExpiration().before(new Date());
  }

  private String generateToken(Map<String, Object> claims, UserDetails userDetails) {
    return Jwts.builder()
        .claims(claims)
        .subject(userDetails.getUsername())
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10))
        .signWith(secretKey, Jwts.SIG.HS256)
        .compact();
  }

  /**
   * Расшифровывает токен и извлекает данные из него.
   *
   * @param token          Токен.
   * @param claimsResolver Разрешитель.
   * @param <T>            Тип данных.
   * @return Данные из токена.
   */
  private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    return claimsResolver.apply(extractAllClaims(token));
  }

  /**
   * Извлекает все данные из токена.
   *
   * @param token Токен.
   * @return Данные из токена.
   */
  private Claims extractAllClaims(String token) {
    var jwtParser = Jwts.parser()
        .verifyWith(secretKey)
        .build();
    return jwtParser
        .parseSignedClaims(token)
        .getPayload();
  }
}
