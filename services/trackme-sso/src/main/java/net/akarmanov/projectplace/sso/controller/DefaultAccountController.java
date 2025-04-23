package net.akarmanov.projectplace.sso.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.akarmanov.projectplace.sso.dto.UserDto;
import net.akarmanov.projectplace.sso.dto.UserUpdateDto;
import net.akarmanov.projectplace.sso.services.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SCOPE_profile')")
public class DefaultAccountController implements AccountController {

  private final AccountService accountService;

  @Override
  public ResponseEntity<UserDto> userInfo(Authentication authentication) {
    var uuid = UUID.fromString(authentication.getName());
    var user = accountService.getUser(uuid);
    return ResponseEntity.ok(user);
  }

  @Override
  public ResponseEntity<Void> update(@Valid UserUpdateDto userDto, Authentication authentication) {
    accountService.updateUser(userDto, authentication);
    return ResponseEntity.ok().build();
  }
}
