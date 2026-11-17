package net.trackme.backend.rest.api.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import net.trackme.backend.services.organization.OrganizationDataService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * Тесты API управления данными организаций в backend.
 */
class OrganizationAdminRestControllerImplTest {

  private final OrganizationDataService organizationDataService =
      mock(OrganizationDataService.class);

  private final OrganizationAdminRestControllerImpl controller =
      new OrganizationAdminRestControllerImpl(organizationDataService);

  @Test
  void deleteOrganizationDataReturnsNoContent() {
    var organizationId = UUID.randomUUID();

    var response = controller.deleteOrganizationData(organizationId);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    verify(organizationDataService).deleteOrganizationData(organizationId);
  }

  @Test
  void renameOrganizationReturnsNoContent() {
    var organizationId = UUID.randomUUID();

    var response = controller.renameOrganization(organizationId, "Новая организация");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    verify(organizationDataService).renameOrganization(organizationId, "Новая организация");
  }
}
