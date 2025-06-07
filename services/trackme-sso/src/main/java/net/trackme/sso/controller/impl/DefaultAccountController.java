package net.trackme.sso.controller.impl;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import net.trackme.sso.controller.AccountController;
import net.trackme.sso.dto.UserDto;
import net.trackme.sso.dto.UserUpdateDto;
import net.trackme.sso.services.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
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
