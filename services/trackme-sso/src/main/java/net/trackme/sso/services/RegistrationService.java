package net.trackme.sso.services;

import jakarta.servlet.http.HttpServletRequest;
import net.trackme.sso.dto.RegistrationRequestDto;

public interface RegistrationService {
  /**
   * Регистрация нового пользователя.
   *
   * @param registrationRequestDto DTO запроса на регистрацию пользователя.
   */
  void register(RegistrationRequestDto registrationRequestDto);

  /**
   * Подтверждение регистрации пользователя.
   *
   * @param token   токен подтверждения.
   * @param request
   */
  void confirm(String token, HttpServletRequest request);
}
