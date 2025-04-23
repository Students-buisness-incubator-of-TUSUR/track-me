package net.akarmanov.projectplace.sso.services;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.akarmanov.projectplace.sso.dto.UserDto;
import net.akarmanov.projectplace.sso.dto.UserUpdateDto;
import net.akarmanov.projectplace.sso.mappers.UserMapper;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DefaultAccountService implements AccountService {

  private final UserService userService;

  private final UserMapper userMapper;

  @Override
  public UserDto getUser(UUID id) {
    var userEntity = userService.findById(id);
    return userMapper.userEntityToUserDto(userEntity);
  }

  @Override
  public void updateUser(@Valid UserUpdateDto userDto, Authentication authentication) {
    var userEntity = userService.findById(UUID.fromString(authentication.getName()));
    userMapper.updateUserEntityFromUserDto(userDto, userEntity);
    userService.save(userEntity);
  }

  @Override
  public void changePassword(String newPassword,
                             String oldPassword, Authentication authentication) {
    var userId = UUID.fromString(authentication.getName());
    userService.changePassword(userId, newPassword, oldPassword);
  }
}
