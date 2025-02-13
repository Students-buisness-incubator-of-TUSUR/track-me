package net.akarmanov.projectplace.rest.api.auth;

import lombok.RequiredArgsConstructor;
import net.akarmanov.projectplace.rest.api.dto.SingInRequest;
import net.akarmanov.projectplace.rest.api.dto.SingUpRequest;
import net.akarmanov.projectplace.services.auth.AuthenticationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class AuthRestControllerImpl implements AuthRestController {
  private final AuthenticationService authenticationService;

  @Override
  public ResponseEntity<Void> singUp(SingUpRequest singUpRequest) {
    authenticationService.singUp(singUpRequest);
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<JwtAuthenticationResponse> singIn(SingInRequest singInRequest) {
    var response = authenticationService.singIn(singInRequest);
    return ResponseEntity.ok(response);
  }
}
