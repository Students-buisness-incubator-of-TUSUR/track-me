package net.trackme.sso.services.impl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.trackme.sso.components.RegistrationStore;
import net.trackme.sso.components.RegistrationTokenStore;
import net.trackme.sso.config.AppProperties;
import net.trackme.sso.dto.RegistrationRequestDto;
import net.trackme.sso.dto.RegistrationToken;
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

  private final UserService userService;

  private final EmailService emailService;

  private final RegistrationStore registrationStore;

  private final RegistrationTokenStore tokenStore;

  private final AppProperties appProperties;

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
      registrationStore.save(requestDto, tokenHash);
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
                "confirmationLink", getConfirmationLink(tokenHash)));
  }

  @Override
  public void confirm(String token, HttpServletRequest request) {
    if (!tokenStore.isTokenValid(token)) {
      throw InformationException.builder("$happened.unexpected.error").build();
    }
    registrationStore.take(token)
        .ifPresentOrElse(userService::saveUser,
                () -> log.error("Registration token not found for tokenHash: {}", token));
  }

  private String getConfirmationLink(String token) {
    var httpUrl = authorizationServerProperties.getIssuer() + "/client/registration-confirm";
    return UriComponentsBuilder.fromUriString(httpUrl, UriComponentsBuilder.ParserType.WHAT_WG)
        .queryParam("token", token)
        .build().toUriString();
  }
}
