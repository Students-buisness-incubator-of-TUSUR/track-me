package net.trackme.sso.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import net.trackme.sso.dao.entity.OrganizationEntity;
import net.trackme.sso.dao.entity.RoleEntity;
import net.trackme.sso.dao.entity.UserEntity;
import net.trackme.sso.dao.repository.UserRepository;
import net.trackme.sso.services.OrganizationAccessNotifier;
import net.trackme.sso.type.OrganizationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.LockedException;

/**
 * Тесты проверки доступа пользователя к организации при входе.
 */
class CustomUserDetailsServiceTest {

  private final UserRepository users = mock(UserRepository.class);

  private final OrganizationAccessNotifier notifier =
      mock(OrganizationAccessNotifier.class);

  private final CustomUserDetailsService service =
      new CustomUserDetailsService(users, notifier);

  @Test
  void userWithoutOrganizationCannotLogInAndIsNotified() {
    var user = user("TRACKER", null);
    when(users.findByUsername(user.getUsername())).thenReturn(java.util.Optional.of(user));

    var error = assertThrows(LockedException.class,
        () -> service.loadUserByUsername(user.getUsername()));

    assertEquals(
        "Пользователю не назначена организация. Обратитесь к владельцу платформы.",
        error.getMessage());
    verify(notifier).notifyAccessDenied(user, null);
  }

  @Test
  void userOfBlockedOrganizationCannotLogInAndIsNotified() {
    var organization = organization(OrganizationStatus.BLOCKED, null);
    var user = user("TRACKER", organization);
    when(users.findByUsername(user.getUsername())).thenReturn(java.util.Optional.of(user));

    var error = assertThrows(LockedException.class,
        () -> service.loadUserByUsername(user.getUsername()));

    assertEquals(
        "Организация заблокирована или срок её действия истёк. "
            + "Обратитесь к владельцу платформы.",
        error.getMessage());
    verify(notifier).notifyAccessDenied(user, organization);
  }

  @Test
  void userOfExpiredOrganizationCannotLogInAndIsNotified() {
    var organization = organization(
        OrganizationStatus.ACTIVE, LocalDate.now().minusDays(1));
    var user = user("TRACKER", organization);
    when(users.findByUsername(user.getUsername())).thenReturn(java.util.Optional.of(user));

    assertThrows(LockedException.class,
        () -> service.loadUserByUsername(user.getUsername()));

    verify(notifier).notifyAccessDenied(user, organization);
  }

  @Test
  void userOfActiveOrganizationCanLogIn() {
    var organization = organization(
        OrganizationStatus.ACTIVE, LocalDate.now().plusDays(1));
    var user = user("TRACKER", organization);
    when(users.findByUsername(user.getUsername())).thenReturn(java.util.Optional.of(user));

    var details = service.loadUserByUsername(user.getUsername());

    assertEquals(user.getUsername(), details.getUsername());
    assertTrue(details.getAuthorities().stream()
        .anyMatch(authority -> "TRACKER".equals(authority.getAuthority())));
    verifyNoInteractions(notifier);
  }

  @Test
  void overlordCanLogInWithoutOrganization() {
    var user = user("OVERLORD", null);
    when(users.findByUsername(user.getUsername())).thenReturn(java.util.Optional.of(user));

    var details = service.loadUserByUsername(user.getUsername());

    assertEquals(user.getUsername(), details.getUsername());
    verify(notifier, never()).notifyAccessDenied(user, null);
  }

  private static UserEntity user(String roleCode, OrganizationEntity organization) {
    var role = new RoleEntity();
    role.setId(UUID.randomUUID());
    role.setCode(roleCode);
    role.setName(roleCode);
    role.setActive(true);

    var user = new UserEntity();
    user.setId(UUID.randomUUID());
    user.setUsername("user-" + roleCode.toLowerCase());
    user.setEmail("user@example.com");
    user.setPasswordHash("hash");
    user.setActive(true);
    user.setAccountNonLocked(true);
    user.setRoles(Set.of(role));
    user.setOrganization(organization);
    return user;
  }

  private static OrganizationEntity organization(OrganizationStatus status,
                                                 LocalDate expirationDate) {
    var organization = new OrganizationEntity();
    organization.setId(UUID.randomUUID());
    organization.setName("Организация");
    organization.setStatus(status);
    organization.setExpirationDate(expirationDate);
    return organization;
  }
}
