package net.trackme.sso.services.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import net.trackme.commons.filters.FilterRequest;
import net.trackme.sso.dao.entity.RoleEntity;
import net.trackme.sso.dao.entity.UserEntity;
import net.trackme.sso.dao.repository.RoleRepository;
import net.trackme.sso.dao.repository.UserRepository;
import net.trackme.sso.dto.RegistrationRequestDto;
import net.trackme.sso.dto.UserDto;
import net.trackme.sso.exception.AuthException;
import net.trackme.sso.exception.EmailNotFoundException;
import net.trackme.sso.exception.WrongOldPasswordException;
import net.trackme.sso.mapper.UserMapper;
import net.trackme.sso.services.UserService;
import net.trackme.sso.type.AuthErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import static net.trackme.sso.dao.UserSpecification.byRole;
import static net.trackme.sso.dao.UserSpecification.withFilters;

@Service
@RequiredArgsConstructor
public class DefaultUserService implements UserService {

  private final UserRepository userRepository;

  private final RoleRepository roleRepository;

  private final PasswordEncoder passwordEncoder;

  private final UserMapper userMapper;

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
  public void resetPassword(String email, String password) {
    var userEntity = findByEmail(email);
    userEntity.setPasswordHash(passwordEncoder.encode(password));
    save(userEntity);
  }

  @Override
  public UserEntity findByUsername(String name) {
    return userRepository.findByUsername(name)
        .orElseThrow(() -> new UsernameNotFoundException(name));
  }

  @Override
  public UserEntity findByEmail(String email) {
    return userRepository.findByEmail(email)
            .orElseThrow(() -> new EmailNotFoundException(email));
  }

  @Override
  @Transactional
  public void enableUser(String username) {
    changeActivity(username, true);
  }

  @Override
  @Transactional
  public void disableUser(String username) {
    var userEntity = findByUsername(username);
    if (Boolean.FALSE.equals(userEntity.getActive())) {
      userEntity.setAccountNonLocked(false);
      save(userEntity);
    }
    changeActivity(username, false);
  }

  @Override
  public UserDto getUserInfo(String username) {
    var userEntity = findByUsername(username);
    return UserDto.builder()
        .username(userEntity.getUsername())
        .email(userEntity.getEmail())
        .fullName(userEntity.getFullName())
        .phoneNumber(userEntity.getPhoneNumber())
        .avatarUrl(userEntity.getAvatarUrl())
        .enabled(userEntity.getActive())
        .roles(userEntity.getRoles().stream()
            .map(RoleEntity::getCode)
            .toList())
        .build();
  }

  @Override
  public Page<UserDto> getTrackers(FilterRequest filterRequest, Pageable pageable) {
    var filters = filterRequest.filters();
    var trackers = userRepository.findAll(
        withFilters(filters).and(byRole("TRACKER")),
        pageable);
    return trackers.map(userMapper::userEntityToUserDto);
  }

  @Override
  public Page<UserDto> getAdmins(FilterRequest filterRequest, Pageable pageable) {
    var filters = filterRequest.filters();
    var admins = userRepository.findAll(
        withFilters(filters).and(byRole("ADMIN")),
        pageable);
    return admins.map(userMapper::userEntityToUserDto);
  }

    @Override
    public boolean existsByEmailOrUsername(String email, String username) {
        return userRepository.existsByEmailOrUsername(email, username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    private void changeActivity(String username, boolean active) {
    var userEntity = findByUsername(username);
    userEntity.setActive(active);
    save(userEntity);
  }
}
