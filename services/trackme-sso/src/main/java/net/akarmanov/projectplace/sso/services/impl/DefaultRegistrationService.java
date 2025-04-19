package net.akarmanov.projectplace.sso.services.impl;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.akarmanov.projectplace.sso.components.RegistrationStore;
import net.akarmanov.projectplace.sso.components.RegistrationTokenStore;
import net.akarmanov.projectplace.sso.dto.RegistrationRequestDto;
import net.akarmanov.projectplace.sso.dto.RegistrationToken;
import net.akarmanov.projectplace.sso.exception.InformationException;
import net.akarmanov.projectplace.sso.services.MailService;
import net.akarmanov.projectplace.sso.services.RegistrationService;
import net.akarmanov.projectplace.sso.services.UserService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultRegistrationService implements RegistrationService {

  private final UserService userService;

  private final RegistrationStore registrationStore;

  private final RegistrationTokenStore tokenStore;

  private final MailService mailService;


  @Override
  public void register(RegistrationRequestDto requestDto, HttpServletResponse response) {
    if (userService.existByEmail(requestDto.email())) {
      throw InformationException.builder("$account.already.exist").build();
    }

    RegistrationToken token = tokenStore.generateToken(response);
    try {
      registrationStore.save(requestDto, token.sessionId());
    } catch (Exception e) {
      throw InformationException.builder("$happened.unexpected.error").build();
    }

    log.info("Registration token = {}. SessionId = {}", token.token(), token.sessionId());
  }

  @Override
  public void confirm(String token) {
    // TODO: implement confirmation logic
  }
}
