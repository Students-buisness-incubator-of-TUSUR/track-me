package net.akarmanov.projectplace.authservice.api;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/v1/registration")
public interface RegistrationRestController {
  @PostMapping("/register")
  ResponseEntity<Void> register(@RequestBody @Valid RegistrationRequest request);
}
