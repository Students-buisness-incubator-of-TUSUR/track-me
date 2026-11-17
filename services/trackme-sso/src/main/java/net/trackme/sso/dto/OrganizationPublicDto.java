package net.trackme.sso.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Краткая информация об организации для выбора при регистрации.
 *
 * @param id   идентификатор организации
 * @param name название организации
 */
@Schema(description = "Организация для выбора при регистрации")
public record OrganizationPublicDto(
    @Schema(description = "Идентификатор организации")
    String id,
    @Schema(description = "Название организации")
    String name
) {
}
