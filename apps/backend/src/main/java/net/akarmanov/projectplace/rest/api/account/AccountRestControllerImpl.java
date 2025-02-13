package net.akarmanov.projectplace.rest.api.account;

import lombok.RequiredArgsConstructor;
import net.akarmanov.projectplace.rest.api.dto.UserDTO;
import net.akarmanov.projectplace.rest.api.dto.UserUpdateDTO;
import net.akarmanov.projectplace.services.user.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AccountRestControllerImpl implements AccountRestController {

  private final AccountService accountService;

  @Override
  public ResponseEntity<UserDTO> getCurrentUserInfo() {
    var userDTO = accountService.getCurrentUserInfo();
    return ResponseEntity.ok(userDTO);
  }

  @Override
  public ResponseEntity<UserDTO> updateCurrentUserInfo(UserUpdateDTO userDTO) {
    var userDto = accountService.updateUserInfo(userDTO);
    return ResponseEntity.ok(userDto);
  }

  @Override
  public ResponseEntity<Void> changePassword(String oldPassword, String newPassword) {
    accountService.changePassword(oldPassword, newPassword);
    return ResponseEntity.noContent().build();
  }
}
