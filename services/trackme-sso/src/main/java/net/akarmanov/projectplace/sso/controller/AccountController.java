package net.akarmanov.projectplace.sso.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import net.akarmanov.projectplace.sso.dto.UserDto;
import net.akarmanov.projectplace.sso.dto.UserUpdateDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "API для управления пользователями")
@RequestMapping("/api/v1/account")
public interface AccountController {

  @GetMapping("/info")
  ResponseEntity<UserDto> userInfo(Authentication authentication);

  @PostMapping("/update")
  ResponseEntity<Void> update(@RequestBody @Valid UserUpdateDto userDto,
                              Authentication authentication);

}
