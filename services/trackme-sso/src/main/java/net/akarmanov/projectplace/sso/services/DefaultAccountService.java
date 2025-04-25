package net.akarmanov.projectplace.sso.services;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.akarmanov.projectplace.sso.dto.UserDto;
import net.akarmanov.projectplace.sso.dto.UserUpdateDto;
import net.akarmanov.projectplace.sso.mappers.UserMapper;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DefaultAccountService implements AccountService {

  private final UserService userService;

  private final UserMapper userMapper;

  @Override
  public UserDto getUser(String username) {
    var userEntity = userService.findByUsername(username);
    return userMapper.userEntityToUserDto(userEntity);
  }

  @Override
  public void updateUser(@Valid UserUpdateDto userDto, Authentication authentication) {
    var userEntity = userService.findByUsername(authentication.getName());
    userMapper.updateUserEntityFromUserDto(userDto, userEntity);
    userService.save(userEntity);
  }

  @Override
  public void changePassword(String newPassword,
                             String oldPassword, Authentication authentication) {
    var username = authentication.getName();
    userService.changePassword(username, newPassword, oldPassword);
  }
}
