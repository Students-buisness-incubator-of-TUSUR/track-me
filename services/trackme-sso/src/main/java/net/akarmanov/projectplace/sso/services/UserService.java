package net.akarmanov.projectplace.sso.services;

import net.akarmanov.projectplace.commons.filters.FilterRequest;
import net.akarmanov.projectplace.sso.dao.entity.UserEntity;
import net.akarmanov.projectplace.sso.dto.AuthProvider;
import net.akarmanov.projectplace.sso.dto.AuthorizedUser;
import net.akarmanov.projectplace.sso.dto.RegistrationRequestDto;
import net.akarmanov.projectplace.sso.dto.UserDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.UUID;

public interface UserService {
  /**
   * Создание или обновление пользователя используя сервис-провайдер
   */
  UserEntity save(OAuth2User userDto, AuthProvider provider);

  /**
   * Создание или обновление пользователя с последующим маппингом в сущность AuthorizedUser
   */
  AuthorizedUser saveAndMap(OAuth2User userDto, AuthProvider provider);

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
