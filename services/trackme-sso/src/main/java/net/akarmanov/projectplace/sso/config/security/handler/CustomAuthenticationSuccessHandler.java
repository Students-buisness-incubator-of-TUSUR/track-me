package net.akarmanov.projectplace.sso.config.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

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


  private final RequestCache requestCache = new HttpSessionRequestCache();

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request,
      HttpServletResponse response,
      Authentication authentication
  ) {
    SavedRequest savedRequest = this.requestCache.getRequest(request, response);
    if (savedRequest == null) {
      response.setHeader(headerName, locationUrl);
    } else {
      this.requestCache.removeRequest(request, response);
      this.clearAuthenticationAttributes(request);
      String targetUrl = savedRequest.getRedirectUrl();
      response.setHeader(headerName, targetUrl);
    }
  }

  /**
   * Удаляем временные атрибуты процесса аутентификации.
   * Решение позаимствовано из {@link org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler}
   */
  private void clearAuthenticationAttributes(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session != null) {
      session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
    }
  }
}
