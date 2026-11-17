package net.trackme.backend.rest.api.admin;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.trackme.backend.services.organization.OrganizationDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Реализация API управления данными организаций.
 */
@RestController
@RequiredArgsConstructor
public class OrganizationAdminRestControllerImpl implements OrganizationAdminRestController {

  private final OrganizationDataService organizationDataService;

  @Override
  public ResponseEntity<Void> deleteOrganizationData(UUID organizationId) {
    organizationDataService.deleteOrganizationData(organizationId);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> renameOrganization(UUID organizationId, String name) {
    organizationDataService.renameOrganization(organizationId, name);
    return ResponseEntity.noContent().build();
  }
}
