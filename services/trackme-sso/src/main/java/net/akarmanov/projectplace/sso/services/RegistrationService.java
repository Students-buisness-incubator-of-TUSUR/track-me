package net.akarmanov.projectplace.sso.services;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.akarmanov.projectplace.sso.dto.RegistrationRequestDto;

public interface RegistrationService {
  /**
   * Регистрация нового пользователя.
   *
   * @param registrationRequestDto DTO запроса на регистрацию пользователя.
   * @param response HTTP ответ.
   */
  void register(RegistrationRequestDto registrationRequestDto, HttpServletResponse response);

  /**
   * Подтверждение регистрации пользователя.
   *
   * @param token   токен подтверждения.
   * @param request
   */
  void confirm(String token, HttpServletRequest request);
}
