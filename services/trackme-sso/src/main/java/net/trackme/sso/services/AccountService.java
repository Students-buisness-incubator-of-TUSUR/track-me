package net.trackme.sso.services;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import net.trackme.sso.dto.UserDto;
import net.trackme.sso.dto.UserUpdateDto;
import org.springframework.security.core.Authentication;

public interface AccountService {
  UserDto getUser(String username);

  void updateUser(@Valid UserUpdateDto userDto, Authentication authentication);

  void changePassword(String newPassword,
                      @NotBlank @Size String oldPassword,
                      Authentication authentication);
}
