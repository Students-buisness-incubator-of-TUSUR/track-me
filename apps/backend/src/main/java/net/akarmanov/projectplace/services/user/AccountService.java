package net.akarmanov.projectplace.services.user;

import net.akarmanov.projectplace.rest.api.dto.UserCreateDto;
import net.akarmanov.projectplace.rest.api.dto.UserDTO;
import net.akarmanov.projectplace.rest.api.dto.UserUpdateDTO;

public interface AccountService {
  UserDTO getCurrentUserInfo();

  UserDTO updateUserInfo(UserUpdateDTO userDTO);

  void changePassword(String oldPassword, String newPassword);

  void registerUser(UserCreateDto userDto);
}
