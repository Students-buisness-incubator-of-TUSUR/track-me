package net.trackme.sso.services;

import net.trackme.commons.filters.FilterRequest;
import net.trackme.sso.dao.entity.UserEntity;
import net.trackme.sso.dto.RegistrationRequestDto;
import net.trackme.sso.dto.UserDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface UserService {

  /**
   * Создание пользователя на основе регистрационных данных. Пользователь будет не активирован.
   *
   * @param userDto данные указанные при регистрации
   */
  UserEntity saveUser(RegistrationRequestDto userDto);

  void save(UserEntity userEntity);

  void changePassword(String username, String newPassword, String oldPassword);

  void resetPassword(String email, String password);

  UserEntity findByUsername(String name);

  UserEntity findByEmail(String email);

  void enableUser(String username);

  void disableUser(String username);

  void unlockUser(String username);

  void deleteUser(String username);

  List<Map<String, String>> getUserTeams(String username);

  UserDto getUserInfo(String username);

  Page<UserDto> getTrackers(FilterRequest filterRequest, Pageable pageable);

  Page<UserDto> getAdmins(FilterRequest filterRequest, Pageable pageable);

  /**
   * Возвращает список суперадминистраторов.
   *
   * @param filterRequest фильтры
   * @param pageable      параметры страницы
   * @return страница суперадминистраторов
   */
  Page<UserDto> getSuperAdmins(FilterRequest filterRequest, Pageable pageable);

  /**
   * Повышает администратора до суперадминистратора.
   *
   * @param username имя пользователя
   */
  void promoteToSuperAdmin(String username);

  boolean existsByEmailOrUsername(String email, String username);

  boolean existsByEmail(String email);
}
