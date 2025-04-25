package net.akarmanov.projectplace.sso.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import net.akarmanov.projectplace.sso.dto.UserDto;
import net.akarmanov.projectplace.sso.dto.UserUpdateDto;
import net.akarmanov.projectplace.sso.services.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SCOPE_profile')")
public class DefaultAccountController implements AccountController {

  private final AccountService accountService;

  @Override
  public ResponseEntity<UserDto> userInfo(Authentication authentication) {
    var user = accountService.getUser(authentication.getName());
    return ResponseEntity.ok(user);
  }

  @Override
  public ResponseEntity<Void> update(@Valid UserUpdateDto userDto, Authentication authentication) {
    accountService.updateUser(userDto, authentication);
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<Void> changePassword(@NotBlank @Size(min = 6) String newPassword,
                                             @NotBlank @Size(min = 6) String oldPassword,
                                             Authentication authentication) {
    accountService.changePassword(newPassword, oldPassword, authentication);
    return ResponseEntity.ok().build();
  }
}
