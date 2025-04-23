package net.akarmanov.projectplace.sso.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import net.akarmanov.projectplace.sso.dto.UserDto;
import net.akarmanov.projectplace.sso.dto.UserUpdateDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Validated
@Tag(name = "API для управления пользователями")
@RequestMapping("/api/v1/account")
public interface AccountController {

  @Operation(summary = "Получение информации о пользователе")
  @GetMapping(path = "/info", produces = "application/json")
  ResponseEntity<UserDto> userInfo(Authentication authentication);

  @Operation(summary = "Обновление информации о пользователе")
  @PostMapping(path = "/update", produces = "application/json", consumes = "application/json")
  ResponseEntity<Void> update(@RequestBody @Valid UserUpdateDto userDto,
                              Authentication authentication);

  @PostMapping("/changePassword")
  ResponseEntity<Void> changePassword(@RequestParam @NotBlank @Size(min = 6) String newPassword,
                                      @RequestParam @NotBlank @Size(min = 6) String oldPassword,
                                      Authentication authentication);

}
