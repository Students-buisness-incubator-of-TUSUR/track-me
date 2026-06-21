package net.trackme.sso.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import net.trackme.sso.dto.RegistrationRequestDto;
import net.trackme.sso.dto.RecoveryPasswordRequestDto;
import net.trackme.sso.dto.ResetPasswordRequestDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "API Регистрации")
@RequestMapping("/api/v1/registration")
public interface RegistrationController {
  @PostMapping("/init")
  @Operation(summary = "Регистрация нового пользователя")
  ResponseEntity<Void> register(@RequestBody @Valid RegistrationRequestDto registrationRequest);

  @PostMapping("/confirm")
  @Operation(summary = "Подтверждение регистрации пользователя")
  ResponseEntity<Void> confirm(@RequestParam(name = "token") String token,
                               HttpServletRequest request);

  @PostMapping("/recovery-password")
  @Operation(summary = "Восстановление пароля")
  ResponseEntity<Void> recoveryPassword(@RequestBody @Valid RecoveryPasswordRequestDto recoveryPasswordRequest);

  @PostMapping("/reset-password")
  @Operation(summary = "Сброс пароля")
  ResponseEntity<Void> resetPassword(@RequestParam(name = "token") String token,
                                     @RequestBody @Valid ResetPasswordRequestDto resetPasswordRequest);
}