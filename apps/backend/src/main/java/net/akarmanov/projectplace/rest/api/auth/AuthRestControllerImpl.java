package net.akarmanov.projectplace.rest.api.auth;

import lombok.RequiredArgsConstructor;
import net.akarmanov.projectplace.rest.api.dto.SingInRequest;
import net.akarmanov.projectplace.rest.api.dto.SingUpRequest;
import net.akarmanov.projectplace.services.auth.AuthenticationService;
import net.akarmanov.projectplace.services.reset.PasswordResetService;
import net.akarmanov.projectplace.services.user.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class AuthRestControllerImpl implements AuthRestController {
  private final AuthenticationService authenticationService;

  private final UserService userService;

  private final PasswordResetService passwordResetService;

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

  @Override
  public ResponseEntity<Void> forgotPassword(ForgotPasswordRequest forgotPasswordRequest) {
    var user = userService.getUserByEmail(forgotPasswordRequest.email());
    passwordResetService.createToken(user);
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<Void> resetPassword(NewPasswordRequest newPasswordRequest) {
    var token = newPasswordRequest.token();
    var password = newPasswordRequest.password();
    passwordResetService.validateToken(token);
    passwordResetService.resetPassword(token, password);
    return ResponseEntity.ok().build();
  }
}
