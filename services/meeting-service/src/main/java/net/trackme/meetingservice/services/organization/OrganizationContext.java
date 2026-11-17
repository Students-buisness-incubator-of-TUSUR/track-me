package net.trackme.meetingservice.services.organization;

import java.util.Optional;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Контекст организации текущего пользователя.
 *
 * <p>Идентификатор организации передаётся в JWT-токене в claim
 * {@code organization_id}. Если claim отсутствует (внутренний вызов,
 * тест с {@code WithMockUser}), ограничения по организации не применяются.</p>
 */
public final class OrganizationContext {

  /** Claim с идентификатором организации. */
  public static final String ORGANIZATION_ID_CLAIM = "organization_id";

  /** Claim с названием организации. */
  public static final String ORGANIZATION_NAME_CLAIM = "organization_name";

  private static final String ROLE_OVERLORD = "ROLE_OVERLORD";

  private static final String AUTHORITY_OVERLORD = "OVERLORD";

  private OrganizationContext() {
  }

  /**
   * Возвращает идентификатор организации текущего пользователя.
   *
   * @return идентификатор организации или пустой {@link Optional}
   */
  public static Optional<UUID> getOrganizationId() {
    return getJwtAuthentication()
        .map(token -> token.getToken().getClaimAsString(ORGANIZATION_ID_CLAIM))
        .filter(value -> value != null && !value.isBlank())
        .flatMap(OrganizationContext::parseUuid);
  }

  /**
   * Возвращает название организации текущего пользователя.
   *
   * @return название организации или пустой {@link Optional}
   */
  public static Optional<String> getOrganizationName() {
    return getJwtAuthentication()
        .map(token -> token.getToken().getClaimAsString(ORGANIZATION_NAME_CLAIM))
        .filter(value -> value != null && !value.isBlank());
  }

  /**
   * Проверяет, является ли текущий пользователь OVERLORD.
   *
   * @return {@code true}, если пользователь имеет роль OVERLORD
   */
  public static boolean isOverlord() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null && authentication.getAuthorities().stream()
        .map(authority -> authority.getAuthority())
        .anyMatch(authority -> ROLE_OVERLORD.equals(authority)
            || AUTHORITY_OVERLORD.equals(authority));
  }

  /**
   * Проверяет, что текущий пользователь имеет доступ к сущности организации.
   *
   * @param organizationId идентификатор организации сущности
   * @throws AccessDeniedException если сущность принадлежит другой организации
   */
  public static void checkOrganizationAccess(UUID organizationId) {
    if (isOverlord() || organizationId == null) {
      return;
    }
    getOrganizationId().ifPresent(currentOrganizationId -> {
      if (!currentOrganizationId.equals(organizationId)) {
        throw new AccessDeniedException(
            "Доступ к данным другой организации запрещён.");
      }
    });
  }

  private static Optional<JwtAuthenticationToken> getJwtAuthentication() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication instanceof JwtAuthenticationToken jwtAuthentication
        ? Optional.of(jwtAuthentication)
        : Optional.empty();
  }

  private static Optional<UUID> parseUuid(String value) {
    try {
      return Optional.of(UUID.fromString(value));
    } catch (IllegalArgumentException exception) {
      return Optional.empty();
    }
  }
}
