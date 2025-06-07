package net.trackme.sso.services;

import net.trackme.backend.commons.filters.FilterRequest;
import net.trackme.sso.dao.entity.UserEntity;
import net.trackme.sso.dto.RegistrationRequestDto;
import net.trackme.sso.dto.UserDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {

  /**
   * Создание пользователя на основе регистрационных данных. Пользователь будет не активирован.
   *
   * @param userDto данные указанные при регистрации
   */
  UserEntity saveUser(RegistrationRequestDto userDto);

  /**
   * Активация пользователя
   *
   * @param username   уникальный идентификатор пользователя
   * @param password пароль пользователя
   */
  UserEntity firstActivation(UUID username, String password);

  /**
   * Создать пользователя и сразу активировать
   */
  UserEntity saveAndActivateUser(RegistrationRequestDto userDto);

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
