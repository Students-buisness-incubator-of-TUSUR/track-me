package net.trackme.sso.services;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.trackme.sso.dao.entity.OrganizationEntity;
import net.trackme.sso.dao.entity.UserEntity;
import net.trackme.sso.dao.repository.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

/**
 * Определяет организацию текущего аутентифицированного пользователя.
 */
@Service
@RequiredArgsConstructor
public class CurrentUserService {

  /**
   * Название роли владельца платформы.
   */
  public static final String OVERLORD = "OVERLORD";

  private static final String ROLE_PREFIX = "ROLE_";
  private static final String ORGANIZATION_ID_CLAIM = "organization_id";
  private static final String ORGANIZATION_NAME_CLAIM = "organization_name";

  private final UserRepository userRepository;

  /**
   * Возвращает идентификатор организации текущего пользователя.
   *
   * @return идентификатор организации или null, если он не определён
   */
  public UUID getOrganizationId() {
    var jwt = currentJwt();
    if (jwt != null) {
      var claim = jwt.getToken().getClaimAsString(ORGANIZATION_ID_CLAIM);
      return claim == null || claim.isBlank() ? null : UUID.fromString(claim);
    }
    return currentUser().map(UserEntity::getOrganization)
        .map(OrganizationEntity::getId)
        .orElse(null);
  }

  /**
   * Возвращает название организации текущего пользователя.
   *
   * @return название организации или null, если оно не определено
   */
  public String getOrganizationName() {
    var jwt = currentJwt();
    if (jwt != null) {
      return jwt.getToken().getClaimAsString(ORGANIZATION_NAME_CLAIM);
    }
    return currentUser().map(UserEntity::getOrganization)
        .map(OrganizationEntity::getName)
        .orElse(null);
  }

  /**
   * Проверяет, является ли текущий пользователь владельцем платформы.
   *
   * @return true для роли OVERLORD
   */
  public boolean isOverlord() {
    var authentication = authentication();
    return authentication != null && authentication.getAuthorities().stream()
        .anyMatch(authority -> (ROLE_PREFIX + OVERLORD).equals(authority.getAuthority())
            || OVERLORD.equals(authority.getAuthority()));
  }

  /**
   * Возвращает bearer-токен текущего пользователя.
   *
   * @return токен или null
   */
  public String getBearerToken() {
    var jwt = currentJwt();
    return jwt == null ? null : jwt.getToken().getTokenValue();
  }

  /**
   * Возвращает имя текущего пользователя.
   *
   * @return имя пользователя или null
   */
  public String getUsername() {
    var authentication = authentication();
    return authentication == null ? null : authentication.getName();
  }

  private Authentication authentication() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
      return null;
    }
    return authentication;
  }

  private JwtAuthenticationToken currentJwt() {
    var authentication = authentication();
    return authentication instanceof JwtAuthenticationToken jwt ? jwt : null;
  }

  private java.util.Optional<UserEntity> currentUser() {
    var username = getUsername();
    return username == null ? java.util.Optional.empty()
        : userRepository.findByUsername(username);
  }
}
