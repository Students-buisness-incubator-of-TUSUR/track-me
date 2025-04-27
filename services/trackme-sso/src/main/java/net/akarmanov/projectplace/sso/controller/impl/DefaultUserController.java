package net.akarmanov.projectplace.sso.controller.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.akarmanov.projectplace.sso.controller.UserController;
import net.akarmanov.projectplace.sso.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class DefaultUserController implements UserController {

  private final UserService userService;

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> enableUser(String username) {
    log.info("Enabling user: {}", username);
    userService.enableUser(username);
    return null;
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> disableUser(String username) {
    log.info("Disabling user: {}", username);
    userService.disableUser(username);
    return null;
  }
}
