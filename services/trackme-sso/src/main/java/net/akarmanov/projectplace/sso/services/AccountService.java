package net.akarmanov.projectplace.sso.services;

import jakarta.validation.Valid;
import net.akarmanov.projectplace.sso.dto.UserDto;
import net.akarmanov.projectplace.sso.dto.UserUpdateDto;
import org.springframework.security.core.Authentication;

import java.util.UUID;

public interface AccountService {
  UserDto getUser(UUID id);

  void updateUser(@Valid UserUpdateDto userDto, Authentication authentication);
}
