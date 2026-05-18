package net.trackme.sso.services.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static net.trackme.sso.dao.UserSpecification.byRole;
import static net.trackme.sso.dao.UserSpecification.withFilters;
@Slf4j 
@Service
@RequiredArgsConstructor
public class DefaultUserService implements UserService {

  private final UserRepository userRepository;

  private static final String RONIN = "ronin";

  private static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";

  private final RoleRepository roleRepository;

  private final PasswordEncoder passwordEncoder;

  private final UserMapper userMapper;

  @Value("${app.services.backend.url}")
  private String backendServiceUrl;

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
      if (RONIN.equals(username)) {
          checkSuperAdmin();
      }
      changeActivity(username, true);
  }

  @Override
  @Transactional
  public void disableUser(String username) {
      if (RONIN.equals(username)) {
          checkSuperAdmin();
      }
      var userEntity = findByUsername(username);
      if (Boolean.FALSE.equals(userEntity.getActive())) {
          userEntity.setAccountNonLocked(false);
          save(userEntity);
      }
      changeActivity(username, false);
  }

  @Override
  @Transactional
  public void unlockUser(String username) {
      if (RONIN.equals(username)) {
          checkSuperAdmin();
      }
      var userEntity = findByUsername(username);
      userEntity.setAccountNonLocked(true);
      userEntity.setActive(false);
      save(userEntity);
      log.info("User {} unlocked", username);
  }

  @Override
  @Transactional
  public void deleteUser(String username) {
      if (RONIN.equals(username)) {
          checkSuperAdmin();
      }
      var userEntity = findByUsername(username);
      
      // Пытаемся переназначить команды, но не блокируем удаление при ошибке
      try {
          reassignTeamsToRonin(username);
      } catch (Exception e) {
          log.error("Failed to reassign teams from {} to ronin, but proceeding with deletion: {}", 
                    username, e.getMessage());
      }
      
      userRepository.delete(userEntity);
      log.info("User {} deleted", username);
  }

  @Override
  public List<Map<String, String>> getUserTeams(String username) {
    try {
      RestClient restClient = RestClient.create(backendServiceUrl);
      return restClient.get()
          .uri("/api/v1/admin/team-cards/by-user?username={username}", username)
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .body(new ParameterizedTypeReference<List<Map<String, String>>>() {});
    } catch (Exception e) {
      log.error("Error getting teams for {}: {}", username, e.getMessage());
      return List.of();
    }
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

  private void checkSuperAdmin() {
      var auth = SecurityContextHolder.getContext().getAuthentication();
      if (auth == null || auth.getAuthorities().stream()
              .noneMatch(a -> a.getAuthority().equals(ROLE_SUPER_ADMIN))) {
          throw new AccessDeniedException("Ronin user can only be modified by SUPER_ADMIN");
      }
  }

  private void reassignTeamsToRonin(String username) {
    try {
        RestClient restClient = RestClient.create(backendServiceUrl);
        Map<String, String> request = Map.of(
            "fromUsername", username,
            "toUsername", RONIN
        );
        restClient.post()
            .uri("/api/v1/admin/team-cards/reassign")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .toBodilessEntity();
        log.info("Teams reassigned from {} to ronin", username);
    } catch (Exception e) {
        log.error("Error reassigning teams from {} to ronin: {}", username, e.getMessage());
        // Не бросаем исключение, чтобы не блокировать удаление пользователя
    }
  }
}
