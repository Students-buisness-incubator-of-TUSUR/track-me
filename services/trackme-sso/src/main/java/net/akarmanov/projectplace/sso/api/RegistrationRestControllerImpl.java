package net.akarmanov.projectplace.sso.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.akarmanov.projectplace.sso.dto.RegistrationRequestDto;
import net.akarmanov.projectplace.sso.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class RegistrationRestControllerImpl implements RegistrationRestController {
  private final UserService userService;

  @Override
  public ResponseEntity<Void> register(RegistrationRequestDto request) {
    log.info("Registering user: {}", request.username());
    userService.saveAndActivateUser(request);
    return ResponseEntity.ok().build();
  }
}
