package net.akarmanov.projectplace.sso.controller.impl;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.akarmanov.projectplace.sso.controller.RegistrationController;
import net.akarmanov.projectplace.sso.dto.RegistrationRequestDto;
import net.akarmanov.projectplace.sso.services.RegistrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class DefaultRegistrationController implements RegistrationController {
  private final RegistrationService registrationService;

  @Override
  public ResponseEntity<Void> register(RegistrationRequestDto registrationRequest,
                                       HttpServletResponse response) {
    log.info("Registering user with email: {}", registrationRequest.email());
    registrationService.register(registrationRequest, response);
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<Void> confirm(String token, HttpServletRequest request) {
    log.info("Confirming user with token: {}", token);
    registrationService.confirm(token, request);
    return ResponseEntity.ok().build();
  }
}
