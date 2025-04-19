package net.akarmanov.projectplace.sso.services;

public interface MailService {
  /**
   * Отправляет электронное письмо с подтверждением регистрации.
   *
   * @param email адрес электронной почты получателя
   * @param token токен подтверждения
   */
  void sendRegistrationConfirmationEmail(String email, String token);
}
