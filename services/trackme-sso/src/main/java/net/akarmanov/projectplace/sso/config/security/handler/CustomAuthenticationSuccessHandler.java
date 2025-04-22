package net.akarmanov.projectplace.sso.config.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Slf4j
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

  /**
   * URL до главной страницы SSO
   **/
  private final String locationUrl;

  /**
   * Имя заголовка, в котором будет передан URL для перенаправления
   **/
  private final String headerName;

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request,
      HttpServletResponse response,
      Authentication authentication
  ) {
    response.setHeader(headerName, locationUrl);
  }
}
