package net.akarmanov.projectplace.authservice.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class RegistrationRestControllerImpl implements RegistrationRestController {
  private final UserDetailsManager userDetailsManager;

  private final PasswordEncoder passwordEncoder;

  @Override
  public ResponseEntity<Void> register(RegistrationRequest request) {
    log.info("Registering user: {}", request.username());
    var user = User.withUsername(request.username())
        .password(passwordEncoder.encode(request.password()))
        .roles(request.roles().toArray(new String[0]))
        .build();
    userDetailsManager.createUser(user);
    return ResponseEntity.ok().build();
  }
}
