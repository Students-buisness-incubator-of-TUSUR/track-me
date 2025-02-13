package net.akarmanov.projectplace.rest.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.akarmanov.projectplace.services.jwt.JwtService;
import net.akarmanov.projectplace.services.user.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static org.springframework.util.StringUtils.hasText;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  public static final String BEARER_PREFIX = "Bearer ";

  private final UserService userService;

  private final JwtService jwtService;

  private final SecurityContextHolderStrategy securityContextHolderStrategy =
      SecurityContextHolder.getContextHolderStrategy();


  @Override
  protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {
    var token = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (!StringUtils.hasText(token) || !StringUtils.startsWithIgnoreCase(token, BEARER_PREFIX)) {
      filterChain.doFilter(request, response);
      return;
    }
    token = token.substring(BEARER_PREFIX.length());
    var username = jwtService.getUsernameFromToken(token);
    if (hasText(username) && SecurityContextHolder.getContext().getAuthentication() == null) {
      var userDetails = userService.loadUserByUsername(username);
      if (jwtService.isTokenValid(token, userDetails)) {
        var context = securityContextHolderStrategy.createEmptyContext();
        var authentication = new UsernamePasswordAuthenticationToken(
            userDetails,
            null,
            userDetails.getAuthorities());

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        context.setAuthentication(authentication);
        securityContextHolderStrategy.setContext(context);
      }
    }
    filterChain.doFilter(request, response);
  }
}
