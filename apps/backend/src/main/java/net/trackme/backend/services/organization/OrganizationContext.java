package net.trackme.backend.services.organization;

import java.util.Optional;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Контекст организации текущего пользователя.
 *
 * <p>Данные об организации передаются в JWT-токене в claims
 * {@code organization_id} и {@code organization_name}. Если токен отсутствует
 * (например, внутренний вызов или тест с {@code WithMockUser}),
 * контекст считается неограниченным.</p>
 */
@Component
public class OrganizationContext {

    /** Claim с идентификатором организации. */
    public static final String ORGANIZATION_ID_CLAIM = "organization_id";

    /** Claim с названием организации. */
    public static final String ORGANIZATION_NAME_CLAIM = "organization_name";

    private static final String ROLE_OVERLORD = "ROLE_OVERLORD";

    private static final String AUTHORITY_OVERLORD = "OVERLORD";

    /**
     * Возвращает идентификатор организации текущего пользователя.
     *
     * @return идентификатор организации или пустой {@link Optional},
     *     если пользователь не ограничен организацией
     */
    public Optional<UUID> getOrganizationId() {
        return getJwtAuthentication()
                .map(token -> token.getToken().getClaimAsString(ORGANIZATION_ID_CLAIM))
                .filter(value -> value != null && !value.isBlank())
                .flatMap(this::parseUuid);
    }

    /**
     * Возвращает название организации текущего пользователя.
     *
     * @return название организации или пустой {@link Optional}
     */
    public Optional<String> getOrganizationName() {
        return getJwtAuthentication()
                .map(token -> token.getToken().getClaimAsString(ORGANIZATION_NAME_CLAIM))
                .filter(value -> value != null && !value.isBlank());
    }

    /**
     * Проверяет, является ли текущий пользователь OVERLORD.
     *
     * @return {@code true}, если пользователь имеет роль OVERLORD
     */
    public boolean isOverlord() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> ROLE_OVERLORD.equals(authority)
                        || AUTHORITY_OVERLORD.equals(authority));
    }

    /**
     * Проверяет, что текущий пользователь имеет доступ к сущности организации.
     *
     * @param organizationId идентификатор организации сущности
     * @throws AccessDeniedException если сущность принадлежит другой организации
     */
    public void checkOrganizationAccess(UUID organizationId) {
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

    private Optional<JwtAuthenticationToken> getJwtAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication instanceof JwtAuthenticationToken jwtAuthentication
                ? Optional.of(jwtAuthentication)
                : Optional.empty();
    }

    private Optional<UUID> parseUuid(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException _) {
            return Optional.empty();
        }
    }
}
