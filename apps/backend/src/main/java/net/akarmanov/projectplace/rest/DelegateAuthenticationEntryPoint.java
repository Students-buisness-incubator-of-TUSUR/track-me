package net.akarmanov.projectplace.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Component
public class DelegateAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final HandlerExceptionResolver exceptionResolver;

  public DelegateAuthenticationEntryPoint(
      @Qualifier("handlerExceptionResolver") @Lazy HandlerExceptionResolver exceptionResolver) {
    this.exceptionResolver = exceptionResolver;
  }

  @Override
  public void commence(HttpServletRequest request, HttpServletResponse response,
                       AuthenticationException authException) {
    exceptionResolver.resolveException(request, response, null, authException);
  }
}
