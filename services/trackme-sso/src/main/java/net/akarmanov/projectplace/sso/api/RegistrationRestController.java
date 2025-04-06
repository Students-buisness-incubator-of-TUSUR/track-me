package net.akarmanov.projectplace.sso.api;

import jakarta.validation.Valid;
import net.akarmanov.projectplace.sso.dto.RegistrationRequestDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/v1/registration")
public interface RegistrationRestController {
  @PostMapping("/register")
  ResponseEntity<Void> register(@RequestBody @Valid RegistrationRequestDto request);
}
