package net.akarmanov.projectplace.sso.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Validated
@Tag(name = "API для управления пользователями")
@RequestMapping("/api/v1/users")
public interface UserController {
  @PostMapping("/enable")
  @Operation(summary = "Включить пользователя")
  ResponseEntity<Void> enableUser(@RequestParam String username);

  @PostMapping("/disable")
  @Operation(summary = "Отключить пользователя")
  ResponseEntity<Void> disableUser(@RequestParam String username);
}
