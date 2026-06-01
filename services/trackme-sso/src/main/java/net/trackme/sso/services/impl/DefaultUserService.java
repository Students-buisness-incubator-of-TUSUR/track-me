package net.trackme.sso.services.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.trackme.commons.filters.FilterRequest;
import static net.trackme.sso.dao.UserSpecification.byRole;
import static net.trackme.sso.dao.UserSpecification.withFilters;
import net.trackme.sso.dao.entity.RoleEntity;
import net.trackme.sso.dao.entity.UserEntity;
import net.trackme.sso.dao.repository.RoleRepository;
import net.trackme.sso.dao.repository.UserRepository;
import net.trackme.sso.dto.RegistrationRequestDto;
import net.trackme.sso.dto.UserDto;
import net.trackme.sso.exception.AuthException;
import net.trackme.sso.exception.EmailNotFoundException;
import net.trackme.sso.exception.TeamReassignmentException;
import net.trackme.sso.exception.WrongOldPasswordException;
import net.trackme.sso.mapper.UserMapper;
import net.trackme.sso.services.BackendClient;
import net.trackme.sso.services.UserService;
import net.trackme.sso.type.AuthErrorCode;

@Slf4j 
@Service
@RequiredArgsConstructor
public class DefaultUserService implements UserService {

  private final UserRepository userRepository;
  private static final String RONIN = "ronin";
  private static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
  private static final String AUTH_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";
  
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;
  private final UserMapper userMapper;
  private final BackendClient backendClient;

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
            disableRonin();
            return;
        }
        
        // Обычная логика для остальных пользователей
        var userEntity = findByUsername(username);
        if (Boolean.FALSE.equals(userEntity.getActive())) {
            userEntity.setAccountNonLocked(false);
            save(userEntity);
        }
        changeActivity(username, false);
  }

  /**
  * Отключение/блокировка пользователя Ronin.
  * Только SUPER_ADMIN.
  */
  private void disableRonin() {
        checkSuperAdmin();
        
        var roninUser = findByUsername(RONIN);
        
        // Если Ronin уже отключен и пытаемся заблокировать
        if (Boolean.FALSE.equals(roninUser.getActive())) {
            log.error("Cannot lock Ronin: Ronin cannot be locked!");
            throw new TeamReassignmentException(
                "Невозможно заблокировать Ronin. Ronin не может быть заблокирован.");
        }
        
        // Проверяем, есть ли у Ronin команды перед отключением
        List<Map<String, String>> roninTeams;
        try {
            roninTeams = getUserTeams(RONIN);
            log.info("Ronin has {} teams", roninTeams.size());
        } catch (TeamReassignmentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get Ronin teams, cannot safely disable: {}", e.getMessage());
            throw new TeamReassignmentException(
                "Невозможно проверить команды Ronin. Попробуйте позже.", e);
        }
        
        if (!roninTeams.isEmpty()) {
            log.error("Cannot disable Ronin: he has {} teams!", roninTeams.size());
            throw new TeamReassignmentException(
                "Невозможно отключить Ronin: у него есть " + roninTeams.size() + 
                " команд. Сначала переназначьте их на другого трекера.");
        }
        
        // Отключаем Ronin
        roninUser.setActive(false);
        save(roninUser);
        log.warn("RONIN USER HAS BEEN DISABLED!");
  }

  @Override
  @Transactional
  public void unlockUser(String username) {
        if (RONIN.equals(username)) {
            checkSuperAdmin();
            
            // Разблокировка Ronin - просто делаем активным
            var userEntity = findByUsername(username);
            userEntity.setAccountNonLocked(true);
            userEntity.setActive(false);
            save(userEntity);
            log.info("Ronin unlocked (but still inactive until enabled)");
            return;
        }
        
        // Обычная логика для остальных пользователей
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
      
      log.info("Starting deletion process for user: {}", username);
      var userEntity = findByUsername(username);
      
      // Переназначаем команды на Ronin (ошибки логируются внутри)
      reassignTeamsToRonin(username);
      
      // Удаляем пользователя
      userRepository.delete(userEntity);
      log.info("User {} deleted successfully", username);
  }

  @Override
  public List<Map<String, String>> getUserTeams(String username) {
    try {
        String token = extractBearerToken();
        List<Map<String, String>> teams = backendClient.getUserTeams(username, token != null ? token : "");
        return teams != null ? teams : java.util.Collections.emptyList();
    } catch (TeamReassignmentException e) {
        throw e;
    } catch (Exception e) {
        log.warn("Backend service unavailable, returning empty teams list for {}: {}", username, e.getMessage());
        return java.util.Collections.emptyList();
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
          UserEntity roninUser = userRepository.findByUsername(RONIN)
              .orElseThrow(() -> new TeamReassignmentException("Ronin user not found"));
          
          log.info("Reassigning teams from '{}' to '{}' (fullName: '{}')", 
              username, RONIN, roninUser.getFullName());
          
          String token = extractBearerToken();
          
          Map<String, String> request = new HashMap<>();
          request.put("fromUsername", username);
          request.put("toUsername", RONIN);
          request.put("toUserFullName", roninUser.getFullName());
          
          backendClient.reassignTeamsToRonin(request, token != null ? token : "");
          log.info("Teams reassigned from {} to ronin successfully", username);
          
      } catch (TeamReassignmentException e) {
          throw e;
      } catch (Exception e) {
          log.warn("Failed to reassign teams from {} to ronin, skipping: {}", username, e.getMessage());
      }
  }

  private String extractBearerToken() {
      var auth = SecurityContextHolder.getContext().getAuthentication();
      
      // Для JWT аутентификации — берём токен напрямую
      if (auth instanceof org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken jwtAuth) {
          return jwtAuth.getToken().getTokenValue();
      }

      // Fallback — берём из заголовка запроса
      try {
          var requestAttributes = RequestContextHolder.getRequestAttributes();
          if (requestAttributes instanceof ServletRequestAttributes servletAttributes) {
              String authorizationHeader = servletAttributes.getRequest().getHeader(AUTH_HEADER);
              if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
                  return authorizationHeader.substring(BEARER_PREFIX.length());
              }
          }
      } catch (Exception e) {
          log.warn("Error extracting bearer token: {}", e.getMessage());
      }

      return null;
  }
}
