package net.akarmanov.projectplace.rest.api.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import net.akarmanov.projectplace.rest.api.dto.SingInRequest;
import net.akarmanov.projectplace.rest.api.dto.SingUpRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Authentication API")
@RequestMapping(value = "/api/v1/auth",
                consumes = "application/json",
                produces = "application/json")
public interface AuthRestController {
  @Operation(summary = "Регистрация пользователя")
  @PostMapping("/sing-up")
  ResponseEntity<Void> singUp(@RequestBody @Valid SingUpRequest singUpRequest);

  @Operation(summary = "Аутентификация пользователя")
  @PostMapping("/sing-in")
  ResponseEntity<JwtAuthenticationResponse> singIn(@RequestBody @Valid SingInRequest singInRequest);
}
