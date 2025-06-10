package net.trackme.sso.controller.impl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.trackme.sso.controller.RegistrationController;
import net.trackme.sso.dto.RegistrationRequestDto;
import net.trackme.sso.services.RegistrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class DefaultRegistrationController implements RegistrationController {
  private final RegistrationService registrationService;

  @Override
  public ResponseEntity<Void> register(RegistrationRequestDto registrationRequest) {
    log.info("Registering user with email: {}", registrationRequest.email());
    registrationService.register(registrationRequest);
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<Void> confirm(String token, HttpServletRequest request) {
    log.info("Confirming user with token: {}", token);
    registrationService.confirm(token, request);
    return ResponseEntity.ok().build();
  }
}
