package net.akarmanov.projectplace.sso.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class UserControllerImpl implements UserController {
  @Override
  public ResponseEntity<Void> confirmUser(String username) {
    return null;
  }

  @Override
  public ResponseEntity<Void> unconfirmUser(String username) {
    return null;
  }

  @Override
  public ResponseEntity<Void> lockUser(String username) {
    return null;
  }

  @Override
  public ResponseEntity<Void> unlockUser(String username) {
    return null;
  }
}
