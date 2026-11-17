package net.trackme.sso.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.trackme.sso.dao.entity.OrganizationEntity;
import net.trackme.sso.dao.entity.UserEntity;
import net.trackme.sso.dao.repository.OrganizationRepository;
import net.trackme.sso.dao.repository.UserRepository;
import net.trackme.sso.dto.OrganizationCreateDto;
import net.trackme.sso.dto.OrganizationUpdateDto;
import net.trackme.sso.services.BackendClient;
import net.trackme.sso.services.CurrentUserService;
import net.trackme.sso.services.MeetingClient;
import net.trackme.sso.services.OrganizationService;
import net.trackme.sso.type.OrganizationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

/**
 * Тесты управления организациями.
 */
class OrganizationServiceImplTest {

  private final OrganizationRepository organizations = mock(OrganizationRepository.class);

  private final UserRepository users = mock(UserRepository.class);

  private final BackendClient backendClient = mock(BackendClient.class);

  private final MeetingClient meetingClient = mock(MeetingClient.class);

  private final CurrentUserService currentUserService = mock(CurrentUserService.class);

  private final OrganizationServiceImpl service = new OrganizationServiceImpl(
      organizations, users, backendClient, meetingClient, currentUserService);

  @Test
  void createTrimsNameAndStartsActiveWithoutExpiration() {
    when(organizations.existsByNameIgnoreCase("ТУСУР")).thenReturn(false);
    when(organizations.save(any())).thenAnswer(invocation -> {
      OrganizationEntity entity = invocation.getArgument(0);
      entity.setId(UUID.randomUUID());
      return entity;
    });

    var created = service.create(new OrganizationCreateDto("  ТУСУР  ", null));

    assertEquals("ТУСУР", created.name());
    assertEquals(OrganizationStatus.ACTIVE, created.status());
    assertFalse(created.expired());
  }

  @Test
  void createRejectsDuplicateName() {
    when(organizations.existsByNameIgnoreCase("ТУСУР")).thenReturn(true);

    assertThrows(IllegalArgumentException.class,
        () -> service.create(new OrganizationCreateDto("ТУСУР", null)));

    verify(organizations, never()).save(any());
  }

  @Test
  void updateAllowsKeepingOwnName() {
    var id = UUID.randomUUID();
    var organization = organization(id, "ТУСУР", OrganizationStatus.ACTIVE, null);
    when(organizations.findById(id)).thenReturn(Optional.of(organization));
    when(organizations.findByNameIgnoreCase("ТУСУР")).thenReturn(Optional.of(organization));
    when(organizations.save(organization)).thenReturn(organization);

    var updated = service.update(id,
        new OrganizationUpdateDto("ТУСУР", LocalDate.now().plusYears(1)));

    assertEquals("ТУСУР", updated.name());
    assertEquals(LocalDate.now().plusYears(1), updated.expirationDate());
  }

  @Test
  void updateRenamesOrganizationInExternalServices() {
    var id = UUID.randomUUID();
    var organization = organization(id, "ТУСУР", OrganizationStatus.ACTIVE, null);
    when(organizations.findById(id)).thenReturn(Optional.of(organization));
    when(organizations.findByNameIgnoreCase("ТУСУР-2")).thenReturn(Optional.empty());
    when(organizations.save(organization)).thenReturn(organization);
    when(currentUserService.getBearerToken()).thenReturn("token");

    var updated = service.update(id, new OrganizationUpdateDto(" ТУСУР-2 ", null));

    assertEquals("ТУСУР-2", updated.name());
    verify(backendClient).renameOrganization(id, "ТУСУР-2", "token");
    verify(meetingClient).renameOrganization(id, "ТУСУР-2", "token");
  }

  @Test
  void externalRenameFailuresDoNotRollbackLocalUpdate() {
    var id = UUID.randomUUID();
    var organization = organization(id, "ТУСУР", OrganizationStatus.ACTIVE, null);
    when(organizations.findById(id)).thenReturn(Optional.of(organization));
    when(organizations.findByNameIgnoreCase("ТУСУР-2")).thenReturn(Optional.empty());
    when(organizations.save(organization)).thenReturn(organization);
    when(currentUserService.getBearerToken()).thenReturn("token");
    org.mockito.Mockito.doThrow(new IllegalStateException("backend unavailable"))
        .when(backendClient).renameOrganization(id, "ТУСУР-2", "token");
    org.mockito.Mockito.doThrow(new IllegalStateException("meeting unavailable"))
        .when(meetingClient).renameOrganization(id, "ТУСУР-2", "token");

    var updated = service.update(id, new OrganizationUpdateDto("ТУСУР-2", null));

    assertEquals("ТУСУР-2", updated.name());
    verify(organizations).save(organization);
  }

  @Test
  void updateRejectsNameOfAnotherOrganization() {
    var id = UUID.randomUUID();
    var organization = organization(id, "ТУСУР", OrganizationStatus.ACTIVE, null);
    when(organizations.findById(id)).thenReturn(Optional.of(organization));
    when(organizations.findByNameIgnoreCase("Другая"))
        .thenReturn(Optional.of(organization(UUID.randomUUID(), "Другая",
            OrganizationStatus.ACTIVE, null)));

    assertThrows(IllegalArgumentException.class,
        () -> service.update(id, new OrganizationUpdateDto("Другая", null)));
  }

  @Test
  void updateMissingOrganizationThrows() {
    var id = UUID.randomUUID();
    when(organizations.findById(id)).thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class,
        () -> service.update(id, new OrganizationUpdateDto("ТУСУР", null)));
  }

  @Test
  void findAvailableReturnsOnlyActiveNonExpiredOrganizations() {
    var active = organization(UUID.randomUUID(), "Активная", OrganizationStatus.ACTIVE, null);
    var blocked = organization(UUID.randomUUID(), "Заблокированная",
        OrganizationStatus.BLOCKED, null);
    var expired = organization(UUID.randomUUID(), "Истёкшая",
        OrganizationStatus.ACTIVE, LocalDate.now().minusDays(1));
    when(organizations.findAllByOrderByNameAsc())
        .thenReturn(List.of(active, blocked, expired));

    var available = service.findAvailable(null);

    assertEquals(1, available.size());
    assertEquals("Активная", available.getFirst().name());
  }

  @Test
  void defaultOrganizationCannotBeDeleted() {
    assertThrows(AccessDeniedException.class,
        () -> service.delete(OrganizationService.DEFAULT_ORGANIZATION_ID, "Организация"));

    verify(backendClient, never()).deleteOrganization(any(), any());
    verify(meetingClient, never()).deleteOrganization(any(), any());
    verify(organizations, never()).delete(any());
  }

  @Test
  void deleteRequiresExactOrganizationNameConfirmation() {
    var id = UUID.randomUUID();
    var organization = organization(id, "ТУСУР", OrganizationStatus.ACTIVE, null);
    when(organizations.findById(id)).thenReturn(Optional.of(organization));

    assertThrows(IllegalArgumentException.class, () -> service.delete(id, "другая"));
    assertThrows(IllegalArgumentException.class, () -> service.delete(id, null));

    verify(organizations, never()).delete(any());
  }

  @Test
  void deleteCascadesExternalDataUsersAndOrganization() {
    var id = UUID.randomUUID();
    var organization = organization(id, "ТУСУР", OrganizationStatus.ACTIVE, null);
    var firstUser = new UserEntity();
    var secondUser = new UserEntity();
    when(organizations.findById(id)).thenReturn(Optional.of(organization));
    when(currentUserService.getBearerToken()).thenReturn("token");
    when(users.findAllByOrganizationId(id)).thenReturn(List.of(firstUser, secondUser));

    service.delete(id, " тусур ");

    verify(backendClient).deleteOrganization(id, "token");
    verify(meetingClient).deleteOrganization(id, "token");
    verify(users).deleteAll(List.of(firstUser, secondUser));
    verify(organizations).delete(organization);
  }

  @Test
  void externalDeletionFailuresDoNotPreventLocalDeletion() {
    var id = UUID.randomUUID();
    var organization = organization(id, "ТУСУР", OrganizationStatus.ACTIVE, null);
    when(organizations.findById(id)).thenReturn(Optional.of(organization));
    when(currentUserService.getBearerToken()).thenReturn("token");
    when(users.findAllByOrganizationId(id)).thenReturn(List.of());
    org.mockito.Mockito.doThrow(new IllegalStateException("backend unavailable"))
        .when(backendClient).deleteOrganization(id, "token");
    org.mockito.Mockito.doThrow(new IllegalStateException("meeting unavailable"))
        .when(meetingClient).deleteOrganization(id, "token");

    service.delete(id, "ТУСУР");

    verify(organizations).delete(organization);
  }

  @Test
  void blockExpiredBlocksOnlyExpiredActiveOrganizations() {
    var expired = organization(UUID.randomUUID(), "Истёкшая",
        OrganizationStatus.ACTIVE, LocalDate.now().minusDays(2));
    when(organizations.findByStatusAndExpirationDateBefore(
        OrganizationStatus.ACTIVE, LocalDate.now()))
        .thenReturn(List.of(expired));

    var blocked = service.blockExpired(LocalDate.now());

    assertEquals(1, blocked);
    assertEquals(OrganizationStatus.BLOCKED, expired.getStatus());
    verify(organizations).saveAll(List.of(expired));
  }

  @Test
  void isAvailableRejectsExpiredAndMissingOrganizations() {
    var expiredId = UUID.randomUUID();
    var missingId = UUID.randomUUID();
    when(organizations.findById(expiredId)).thenReturn(Optional.of(
        organization(expiredId, "ТУСУР", OrganizationStatus.ACTIVE,
            LocalDate.now().minusDays(1))));
    when(organizations.findById(missingId)).thenReturn(Optional.empty());

    assertFalse(service.isAvailable(expiredId));
    assertFalse(service.isAvailable(missingId));
  }

  private static OrganizationEntity organization(UUID id, String name,
                                                  OrganizationStatus status,
                                                  LocalDate expirationDate) {
    var organization = new OrganizationEntity();
    organization.setId(id);
    organization.setName(name);
    organization.setStatus(status);
    organization.setExpirationDate(expirationDate);
    return organization;
  }
}
