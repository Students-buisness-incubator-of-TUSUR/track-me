package net.akarmanov.projectplace.sso.services.impl;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.akarmanov.projectplace.sso.components.RegistrationStore;
import net.akarmanov.projectplace.sso.components.RegistrationTokenStore;
import net.akarmanov.projectplace.sso.config.AppProperties;
import net.akarmanov.projectplace.sso.dto.RegistrationRequestDto;
import net.akarmanov.projectplace.sso.dto.RegistrationToken;
import net.akarmanov.projectplace.sso.exception.ConfirmRegistrationException;
import net.akarmanov.projectplace.sso.exception.InformationException;
import net.akarmanov.projectplace.sso.services.EmailService;
import net.akarmanov.projectplace.sso.services.RegistrationService;
import net.akarmanov.projectplace.sso.services.UserService;
import org.springframework.boot.autoconfigure.security.oauth2.server.servlet.OAuth2AuthorizationServerProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultRegistrationService implements RegistrationService {

  private final UserService userService;

  private final EmailService emailService;

  private final RegistrationStore registrationStore;

  private final RegistrationTokenStore tokenStore;

  private final AppProperties appProperties;

  private final OAuth2AuthorizationServerProperties authorizationServerProperties;


  @Override
  public void register(RegistrationRequestDto requestDto, HttpServletResponse response) {
    if (userService.existByEmail(requestDto.email())) {
      throw InformationException.builder("$account.already.exist").build();
    }

    RegistrationToken registrationToken = tokenStore.generateToken(response);
    var sessionId = registrationToken.sessionId();
    var token = registrationToken.token();

    try {
      registrationStore.save(requestDto, sessionId);
    } catch (Exception e) {
      throw InformationException.builder("$happened.unexpected.error").build();
    }

    log.info("Registration token = {}. SessionId = {}", token, sessionId);

    emailService.sendMail(
        requestDto.email(),
        appProperties.getMail().getFrom(),
        "[" + appProperties.getMail().getSubject() + "] Подтверждение регистрации",
        "email-confirmation.html",
        Map.of(
            "email", requestDto.email(),
            "token", token,
            "appName", appProperties.getMail().getSubject(),
            "supportEmail", appProperties.getMail().getFrom(),
            "confirmationLink", getConfirmationLink(token)));
  }

  @Override
  public void confirm(String token, HttpServletRequest request) {
    if (tokenStore.isTokenValid(token, request)) {
      throw InformationException.builder("$happened.unexpected.error").build();
    }
    var sessionId = tokenStore.getSessionId(request);

    if (sessionId == null) {
      throw new ConfirmRegistrationException("$registration.confirm.error.no.session.id");
    }

    registrationStore.take(sessionId)
        .ifPresentOrElse(userService::saveUser,
            () -> log.error("Registration token not found for sessionId: {}", sessionId));
  }

  private String getConfirmationLink(String token) {
    var httpUrl = authorizationServerProperties.getIssuer() + "/client/registration-confirm";
    return UriComponentsBuilder.fromUriString(httpUrl, UriComponentsBuilder.ParserType.WHAT_WG)
        .queryParam("token", token)
        .build().toUriString();
  }
}
