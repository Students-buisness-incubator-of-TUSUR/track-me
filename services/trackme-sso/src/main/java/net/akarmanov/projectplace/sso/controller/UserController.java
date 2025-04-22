package net.akarmanov.projectplace.sso.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "API для управления пользователями")
@RequestMapping("/api/v1/users")
public interface UserController {

  @PostMapping("/confirm")
  ResponseEntity<Void> confirmUser(@RequestParam String username);

  @PostMapping("/unconfirm")
  ResponseEntity<Void> unconfirmUser(@RequestParam String username);

  @PostMapping("/lock")
  ResponseEntity<Void> lockUser(@RequestParam String username);

  @PostMapping("/unlock")
  ResponseEntity<Void> unlockUser(@RequestParam String username);

}
