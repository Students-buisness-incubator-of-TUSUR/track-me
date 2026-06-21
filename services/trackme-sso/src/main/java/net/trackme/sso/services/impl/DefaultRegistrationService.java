package net.trackme.sso.services.impl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.trackme.sso.components.RegistrationStore;
import net.trackme.sso.components.RegistrationTokenStore;
import net.trackme.sso.config.AppProperties;
import net.trackme.sso.dto.RegistrationRequestDto;
import net.trackme.sso.dto.RegistrationToken;
import net.trackme.sso.dto.RecoveryPasswordRequestDto;
import net.trackme.sso.dto.ResetPasswordRequestDto;
import net.trackme.sso.exception.InformationException;
import net.trackme.sso.services.EmailService;
import net.trackme.sso.services.RegistrationService;
import net.trackme.sso.services.UserService;
import org.springframework.boot.autoconfigure.security.oauth2.server.servlet.OAuth2AuthorizationServerProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultRegistrationService implements RegistrationService {

  /**
   * Сервис пользователей.
   */
  private final UserService userService;

  /**
   * Сервис электронной почты.
   */
  private final EmailService emailService;

  /**
   * Хранилище данных при регистрации.
   */
  private final RegistrationStore registrationStore;

  /**
   * Хранилище токенов при регистрации.
   */
  private final RegistrationTokenStore tokenStore;

  /**
   * Настройки приложения.
   */
  private final AppProperties appProperties;

  /**
   * Настройки авторизации.
   */
  private final OAuth2AuthorizationServerProperties authorizationServerProperties;


  @Override
  public void register(RegistrationRequestDto requestDto) {
      if (userService.existsByEmailOrUsername(requestDto.email(), requestDto.username())) {
        throw InformationException.builder("$account.already.exist").build();
      }

    RegistrationToken registrationToken = tokenStore.generateToken();
    var tokenHash = registrationToken.tokenHash();
    var token = registrationToken.token();

    try {
      registrationStore.saveToRegistration(requestDto, tokenHash);
    } catch (Exception e) {
      throw InformationException.builder("$happened.unexpected.error").build();
    }

    log.info("Registration token = {}. Hash = {}", token, tokenHash);

    emailService.sendMail(
        requestDto.email(),
        appProperties.getMail().getFrom(),
        "[" + appProperties.getMail().getSubject() + "] Подтверждение регистрации",
        "email-confirmation.html",
        Map.of(
            "email", requestDto.email(),
            "appName", appProperties.getMail().getSubject(),
            "supportEmail", appProperties.getMail().getFrom(),
                "confirmationLink", getConfirmationLink(tokenHash, "/client/registration-confirm"),
                "notificationBotLink", getNotificationBotLink()));
  }

  @Override
  public void confirm(String token, HttpServletRequest request) {
    if (!tokenStore.isTokenValid(token)) {
      throw InformationException.builder("$happened.unexpected.error").build();
    }
    registrationStore.takeToRegistration(token)
        .ifPresentOrElse(userService::saveUser,
                () -> log.error("Registration token not found for tokenHash: {}", token));
  }

  @Override
  public void recoveryPassword(RecoveryPasswordRequestDto requestDto) {
    if (!userService.existsByEmail(requestDto.email())) {
      throw InformationException.builder("$account.does.not.exist").build();
    }

    RegistrationToken registrationToken = tokenStore.generateToken();
    var tokenHash = registrationToken.tokenHash();
    var token = registrationToken.token();

    try {
      registrationStore.saveToRecovery(requestDto, tokenHash);
    } catch (Exception e) {
      throw InformationException.builder("$happened.unexpected.error").build();
    }

    log.info("Recovery password token = {}. Hash = {}", token, tokenHash);
    emailService.sendMail(
            requestDto.email(),
            appProperties.getMail().getFrom(),
            "[" + appProperties.getMail().getSubject() + "] Восстановление пароля",
            "email-confirmation-reset-password.html",
            Map.of(
                    "email", requestDto.email(),
                    "appName", appProperties.getMail().getSubject(),
                    "supportEmail", appProperties.getMail().getFrom(),
                    "confirmationLink", getConfirmationLink(tokenHash, "/client/reset")));
  }

  @Override
  public void resetPassword(String token, ResetPasswordRequestDto requestDto) {
    if (!tokenStore.isTokenValid(token)) {
      throw InformationException.builder("$happened.unexpected.error").build();
    }
    registrationStore.takeToRecovery(token)
      .ifPresentOrElse(dto ->
              userService.resetPassword(dto.email(), requestDto.password()),
              () -> log.error("Recovery token not found for tokenHash: {}", token));
  }

  private String getConfirmationLink(String token, String url) {
    var httpUrl = authorizationServerProperties.getIssuer() + url;
    return UriComponentsBuilder.fromUriString(httpUrl, UriComponentsBuilder.ParserType.WHAT_WG)
        .queryParam("token", token)
        .build().toUriString();
  }

  private String getNotificationBotLink() {
    var httpUrl = appProperties.getTelegramUrl() + "/{botUsername}";
    return UriComponentsBuilder.fromUriString(httpUrl, UriComponentsBuilder.ParserType.WHAT_WG)
            .queryParam("start", "{start}")
            .build(appProperties.getBotUsername(), System.currentTimeMillis()).toString();
  }
}
