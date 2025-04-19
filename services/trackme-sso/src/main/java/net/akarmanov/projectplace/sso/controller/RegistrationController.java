package net.akarmanov.projectplace.sso.controller;


import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import net.akarmanov.projectplace.sso.dto.RegistrationRequestDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "API Регистрации")
@RequestMapping("/api/v1/registration")
public interface RegistrationController {
  @PostMapping("/init")
  ResponseEntity<Void> register(@RequestBody @Valid RegistrationRequestDto registrationRequest,
                                HttpServletResponse response);

  @PostMapping("/confirm")
  ResponseEntity<Void> confirm(@RequestParam(name = "token") String token);
}