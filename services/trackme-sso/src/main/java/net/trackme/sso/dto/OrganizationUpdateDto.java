package net.trackme.sso.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Запрос на изменение организации.
 *
 * @param name           новое название организации
 * @param expirationDate новая дата окончания действия (null — бессрочно)
 */
@Schema(description = "Запрос на изменение организации")
public record OrganizationUpdateDto(
    @Schema(description = "Название организации", example = "ТУСУР")
    @NotBlank(message = "Название организации не может быть пустым.")
    @Size(max = 255, message = "Название организации не должно превышать 255 символов.")
    String name,
    @Schema(description = "Дата окончания действия организации")
    LocalDate expirationDate
) {
}
