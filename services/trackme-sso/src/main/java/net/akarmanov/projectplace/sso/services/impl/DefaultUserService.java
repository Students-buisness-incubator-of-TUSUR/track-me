package net.akarmanov.projectplace.sso.services.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import net.akarmanov.projectplace.sso.dao.entity.UserEntity;
import net.akarmanov.projectplace.sso.dao.repository.RoleRepository;
import net.akarmanov.projectplace.sso.dao.repository.UserRepository;
import net.akarmanov.projectplace.sso.dto.AuthProvider;
import net.akarmanov.projectplace.sso.dto.AuthorizedUser;
import net.akarmanov.projectplace.sso.dto.RegistrationRequestDto;
import net.akarmanov.projectplace.sso.exception.AuthException;
import net.akarmanov.projectplace.sso.exception.WrongOldPasswordException;
import net.akarmanov.projectplace.sso.mapper.AuthorizedUserMapper;
import net.akarmanov.projectplace.sso.services.UserService;
import net.akarmanov.projectplace.sso.type.AuthErrorCode;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DefaultUserService implements UserService {

  private final UserRepository userRepository;

  private final RoleRepository roleRepository;

  private final PasswordEncoder passwordEncoder;

  /**
   * Создание или обновление пользователя используя сервис-провайдер
   */
  @Override
  public UserEntity save(OAuth2User userDto, AuthProvider provider) {
    return switch (provider) {
      case GITHUB -> this.saveUserFromGithub(userDto);
      case GOOGLE -> this.saveUserFromGoogle(userDto);
      case TELEGRAM -> null;
    };
  }

  /**
   * Создание или обновление пользователя с последующим маппингом в сущность AuthorizedUser
   */
  @Override
  public AuthorizedUser saveAndMap(OAuth2User userDto, AuthProvider provider) {
    UserEntity entity = this.save(userDto, provider);
    return AuthorizedUserMapper.map(entity);
  }


  /**
   * Метод описывающий создание/обновление UserEntity на основе OAuth2User полученного из провайдера Github
   */
  private UserEntity saveUserFromGithub(OAuth2User userDto) {
    String email = userDto.getAttribute("email");           // пытаемся получить атрибут email
    UserEntity user = this.getEntityByEmail(email);

    if (userDto.getAttribute("name") != null) {             // получаем firstName, lastName и middleName
      String name = userDto.getAttribute("name");
      user.setFullName(name);

    } else {                                                      // иначе устанавливаем в эти поля значение email
      user.setFullName(userDto.getAttribute("login"));
    }

    if (userDto.getAttribute("avatar_url") !=
        null) {       // если есть аватар, то устанавливаем значение в поле avatarUrl
      user.setAvatarUrl(userDto.getAttribute("avatar_url"));
    }
    return userRepository.save(user);                             // сохраняем сущность UserEntity
  }

  /**
   * Метод описывающий создание/обновление UserEntity на основе OAuth2User полученного из провайдера Google
   */
  private UserEntity saveUserFromGoogle(OAuth2User userDto) {
    String email = userDto.getAttribute("email");
    UserEntity user = this.getEntityByEmail(email);

    if (userDto.getAttribute("given_name") != null) {
      user.setFullName(userDto.getAttribute("given_name"));
    }

    if (userDto.getAttribute("picture") != null) {
      user.setAvatarUrl(userDto.getAttribute("picture"));
    }

    return userRepository.save(user);
  }

  /**
   * Метод получения сущности UserEntity по email
   * Если пользователь с данным email не найден в БД, то создаём новую сущность
   */
  private UserEntity getEntityByEmail(String email) {
    if (email == null) {
      throw new AuthException(AuthErrorCode.EMAIL_IS_EMPTY);
    }
    Optional<UserEntity> userOptional = this.userRepository.findByEmail(email);
    if (userOptional.isEmpty()) {
      var user = new UserEntity();
      user.setEmail(email);
      user.setUsername(email.substring(0, email.indexOf("@")));
      user.setActive(true);
      // добавляем роль по умолчанию
//       TODO: добавить роль по умолчанию
//      user.setRoles(Set.of(roleRepository.findByCode("TRACKER")));
      return userRepository.save(user);
    }
    return userOptional.get();
  }

  /**
   * Создание пользователя на основе регистрационных данных. Пользователь будет не активирован.
   *
   * @param userDto данные указанные при регистрации
   */
  @Override
  @Transactional
  public UserEntity saveUser(RegistrationRequestDto userDto) {
    UserEntity user = new UserEntity();
    user.setEmail(userDto.email());
    user.setUsername(userDto.username());
    user.setFullName(userDto.fullName());
    user.setPhoneNumber(userDto.phoneNumber());
    user.setActive(false);
    user.getRoles().add(roleRepository.findByCode(userDto.role())
        .orElseThrow(() -> new AuthException(AuthErrorCode.ROLE_NOT_FOUND)));
    user.setPasswordHash(passwordEncoder.encode(userDto.password()));
    return userRepository.save(user);
  }

  /**
   * Активация пользователя
   *
   * @param username уникальный идентификатор пользователя
   * @param password пароль пользователя
   */
  @Override
  @Transactional
  public UserEntity firstActivation(UUID username, String password) {
    Optional<UserEntity> userEntityOptional = this.userRepository.findById(username);
    if (userEntityOptional.isEmpty()) {
      throw new AuthException(AuthErrorCode.USER_ACTIVATION_FAILED);
    }
    UserEntity userEntity = userEntityOptional.get();
    userEntity.setPasswordHash(passwordEncoder.encode(password));
    userEntity.setActive(true);
    return userRepository.save(userEntity);
  }

  /**
   * Создать пользователя и сразу активировать
   */
  @Override
  @Transactional
  public UserEntity saveAndActivateUser(RegistrationRequestDto userDto) {
    UserEntity user = saveUser(userDto);
    return firstActivation(user.getId(), userDto.password());
  }

  /**
   * Проверить существует ли пользователь с указанным email
   */
  @Override
  public boolean existByEmail(String email) {
    return userRepository.existsByEmail(email);
  }

  @Override
  public void save(UserEntity userEntity) {
    Assert.notNull(userEntity, "UserEntity must not be null");
    userRepository.save(userEntity);
  }

  @Override
  public void changePassword(String username, String newPassword, String oldPassword) {
    var userEntity = findByUsername(username);
    if (!passwordEncoder.matches(oldPassword, userEntity.getPasswordHash())) {
      throw new WrongOldPasswordException("$password.wrong");
    }
    userEntity.setPasswordHash(passwordEncoder.encode(newPassword));
    save(userEntity);
  }

  @Override
  public UserEntity findByUsername(String name) {
    return userRepository.findByUsername(name)
        .orElseThrow(() -> new UsernameNotFoundException(name));
  }

  @Override
  @Transactional
  public void enableUser(String username) {
    changeActivity(username, true);
  }

  @Override
  @Transactional
  public void disableUser(String username) {
    changeActivity(username, false);
  }

  private void changeActivity(String username, boolean active) {
    var userEntity = findByUsername(username);
    userEntity.setActive(active);
    save(userEntity);
  }
}
