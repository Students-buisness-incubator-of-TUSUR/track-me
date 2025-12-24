package net.trackme.sso.services;

import jakarta.servlet.http.HttpServletRequest;
import net.trackme.sso.dto.RegistrationRequestDto;
import net.trackme.sso.dto.RecoveryPasswordRequestDto;
import net.trackme.sso.dto.ResetPasswordRequestDto;

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

  /**
   * Восстановление пароля.
   * @param recoveryPasswordRequestDto Dto запроса на восстановление пароля.
   */
  void recoveryPassword(RecoveryPasswordRequestDto recoveryPasswordRequestDto);

  /**
   * Сброс пароля.
   * @param token Токен подтверждения.
   * @param resetPasswordRequestDto Dto запроса на сброс пароля.
   */
  void resetPassword(String token, ResetPasswordRequestDto resetPasswordRequestDto);
}
