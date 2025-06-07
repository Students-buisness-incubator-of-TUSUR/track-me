package net.trackme.backend.configuration.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccessTokenRedisValidationFilterTest {

  @Mock
  private RedisTemplate<String, String> redisTemplate;

  @Mock
  private JwtDecoder jwtDecoder;

  @InjectMocks
  private AccessTokenRedisValidationFilter filter;

  @Mock
  private FilterChain filterChain;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Test
  void allowsRequestWhenAuthorizationIdExistsInRedis() throws Exception {
    String token = "validToken";
    String authorizationId = "auth123";
    String redisKey = "oauth2_authorization_complete:" + authorizationId;

    when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
    when(jwtDecoder.decode(token)).thenReturn(Jwt.withTokenValue(token)
        .claim("authorization_id", authorizationId)
        .header("alg", "none")
        .build());
    when(redisTemplate.hasKey(redisKey)).thenReturn(true);

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verify(response, never()).sendError(anyInt(), anyString());
  }

  @Test
  void deniesRequestWhenAuthorizationIdDoesNotExistInRedis() throws Exception {
    String token = "validToken";
    String authorizationId = "auth123";
    String redisKey = "oauth2_authorization_complete:" + authorizationId;

    when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
    when(jwtDecoder.decode(token)).thenReturn(Jwt.withTokenValue(token)
        .claim("authorization_id", authorizationId)
        .header("alg", "none")
        .build());
    when(redisTemplate.hasKey(redisKey)).thenReturn(false);

    filter.doFilterInternal(request, response, filterChain);

    verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token has been revoked");
    verify(filterChain, never()).doFilter(request, response);
  }

  @Test
  void deniesRequestWhenTokenIsInvalid() throws Exception {
    String token = "invalidToken";

    when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
    when(jwtDecoder.decode(token)).thenThrow(new RuntimeException("Invalid token"));

    filter.doFilterInternal(request, response, filterChain);

    verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
    verify(filterChain, never()).doFilter(request, response);
  }

  @Test
  void allowsRequestWhenAuthorizationHeaderIsMissing() throws Exception {
    when(request.getHeader("Authorization")).thenReturn(null);

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verify(response, never()).sendError(anyInt(), anyString());
  }

  @Test
  void allowsRequestWhenAuthorizationHeaderDoesNotStartWithBearer() throws Exception {
    when(request.getHeader("Authorization")).thenReturn("Basic someToken");

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verify(response, never()).sendError(anyInt(), anyString());
  }
}