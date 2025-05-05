package net.akarmanov.projectplace.sso.services;

import net.akarmanov.projectplace.commons.filters.FilterRequest;
import net.akarmanov.projectplace.sso.dao.entity.UserEntity;
import net.akarmanov.projectplace.sso.dto.RegistrationRequestDto;
import net.akarmanov.projectplace.sso.dto.UserDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

  /**
   * Создание пользователя на основе регистрационных данных. Пользователь будет не активирован.
   *
   * @param userDto данные указанные при регистрации
   */
  void saveUser(RegistrationRequestDto userDto);

  /**
   * Проверить существует ли пользователь с указанным email
   */
  boolean existByEmail(String email);

  void save(UserEntity userEntity);

  void changePassword(String username, String newPassword, String oldPassword);

  UserEntity findByUsername(String name);

  void enableUser(String username);

  void disableUser(String username);

  UserDto getUserInfo(String username);

  Page<UserDto> getTrackers(FilterRequest filterRequest, Pageable pageable);

  Page<UserDto> getAdmins(FilterRequest filterRequest, Pageable pageable);
}
