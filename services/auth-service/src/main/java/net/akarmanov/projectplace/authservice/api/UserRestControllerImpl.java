package net.akarmanov.projectplace.authservice.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserRestControllerImpl implements UserRestController {

  private final UserDetailsManager userDetailsManager;

  public UserRestControllerImpl(UserDetailsManager userDetailsManager) {
    this.userDetailsManager = userDetailsManager;
  }

  @Override
  public ResponseEntity<Void> updateUser(UserUpdateDto userUpdateDto) {
    var user = User.builder()
        .username(userUpdateDto.username())
        .password(userUpdateDto.password())
        .roles(userUpdateDto.roles().toArray(new String[0]))
        .build();
    if (userDetailsManager.userExists(user.getUsername())) {
      userDetailsManager.updateUser(user);
    } else {
      userDetailsManager.createUser(user);
    }
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}
