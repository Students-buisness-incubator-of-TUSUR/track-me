package net.trackme.meetingservice.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * API управления данными организаций в meeting-сервисе.
 */
@Tag(name = "Organization API", description = "API для управления данными организаций")
@RequestMapping("/api/v1/admin/organizations")
public interface OrganizationAdminRestController {

  /**
   * Удаляет данные организации в meeting-сервисе.
   *
   * @param organizationId идентификатор организации
   * @return пустой ответ
   */
  @Operation(summary = "Удалить данные организации")
  @DeleteMapping("/{organizationId}")
  ResponseEntity<Void> deleteOrganizationData(
      @Parameter(description = "Идентификатор организации", required = true)
      @PathVariable UUID organizationId);

  /**
   * Обновляет название организации во встречах.
   *
   * @param organizationId идентификатор организации
   * @param name           новое название организации
   * @return пустой ответ
   */
  @Operation(summary = "Обновить название организации во встречах")
  @PatchMapping("/{organizationId}/name")
  ResponseEntity<Void> renameOrganization(
      @Parameter(description = "Идентификатор организации", required = true)
      @PathVariable UUID organizationId,
      @RequestParam @NotBlank String name);
}
