package net.akarmanov.projectplace.services.user;

import lombok.RequiredArgsConstructor;
import net.akarmanov.projectplace.mapping.UserMapper;
import net.akarmanov.projectplace.rest.api.dto.UserDTO;
import net.akarmanov.projectplace.rest.api.dto.UserUpdateDTO;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

  private final UserService userService;

  private final UserMapper userMapper;

  @Override
  public UserDTO getCurrentUserInfo() {
    var user = userService.getCurrentUser();
    return userMapper.mapUserToDto(user);
  }

  @Override
  public UserDTO updateUserInfo(UserUpdateDTO userDTO) {
    var user = userService.getCurrentUser();
    userMapper.updateFromDto(userDTO, user);
    var saved = userService.updateUser(user.getId(), user);
    return userMapper.mapUserToDto(saved);
  }

  @Override
  public void changePassword(String oldPassword, String newPassword) {
    userService.changePassword(oldPassword, newPassword);
  }
}
