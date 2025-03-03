package net.akarmanov.projectplace.configuration;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;

/**
 * Настройки JWT.
 */
@Configuration
public class JwtConfiguration {

  /**
   * Секретный ключ.
   */
  @Value("${JWT_SECRET}")
  private String jwtSecret;

  /**
   * Секретный ключ.
   *
   * @return объект {@link SecretKey}.
   */
  @Bean
  public SecretKey secretKey() {
    var bytes = Decoders.BASE64.decode(jwtSecret);
    return Keys.hmacShaKeyFor(bytes);
  }
}
