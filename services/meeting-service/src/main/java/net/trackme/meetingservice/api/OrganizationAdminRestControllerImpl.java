package net.trackme.meetingservice.api;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.trackme.meetingservice.services.organization.OrganizationDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * Реализация API управления данными организаций в meeting-сервисе.
 */
@RestController
@RequiredArgsConstructor
public class OrganizationAdminRestControllerImpl implements OrganizationAdminRestController {

  private final OrganizationDataService organizationDataService;

  @Override
  @PreAuthorize("hasRole('OVERLORD')")
  public ResponseEntity<Void> deleteOrganizationData(UUID organizationId) {
    organizationDataService.deleteOrganizationData(organizationId);
    return ResponseEntity.noContent().build();
  }

  @Override
  @PreAuthorize("hasRole('OVERLORD')")
  public ResponseEntity<Void> renameOrganization(UUID organizationId, String name) {
    organizationDataService.renameOrganization(organizationId, name);
    return ResponseEntity.noContent().build();
  }
}
