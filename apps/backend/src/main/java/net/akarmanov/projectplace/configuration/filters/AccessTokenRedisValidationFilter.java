package net.akarmanov.projectplace.configuration.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AccessTokenRedisValidationFilter extends OncePerRequestFilter {

  public static final String AUTHORIZATION_COMPLETE = "oauth2_authorization_complete:";

  private final RedisTemplate<String, String> redisTemplate;

  private final JwtDecoder jwtDecoder;

  @Override
  protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

    String authHeader = request.getHeader("Authorization");

    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      String token = authHeader.substring(7);

      try {
        // Декодируем токен
        var jwt = jwtDecoder.decode(token);

        // Извлекаем claim authorization_id
        String authorizationId = jwt.getClaim("authorization_id");

        // Проверяем наличие записи в Redis
        String redisKey = AUTHORIZATION_COMPLETE + authorizationId;
        Boolean exists = redisTemplate.hasKey(redisKey);

        if (Boolean.FALSE.equals(exists)) {
          response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token has been revoked");
          return;
        }
      } catch (Exception ex) {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
        return;
      }
    }

    filterChain.doFilter(request, response);
  }
}
