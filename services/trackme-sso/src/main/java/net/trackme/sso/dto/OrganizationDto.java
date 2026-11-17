package net.trackme.sso.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Builder;
import net.trackme.sso.type.OrganizationStatus;

/**
 * Информация об организации.
 *
 * @param id             идентификатор
 * @param name           название
 * @param status         статус
 * @param expirationDate дата окончания действия (null — бессрочно)
 * @param expired        признак истёкшего срока действия
 */
@Builder
@Schema(description = "Информация об организации")
public record OrganizationDto(
    @Schema(description = "Идентификатор организации")
    String id,
    @Schema(description = "Название организации")
    String name,
    @Schema(description = "Статус организации")
    OrganizationStatus status,
    @Schema(description = "Дата окончания действия организации")
    LocalDate expirationDate,
    @Schema(description = "Признак истёкшего срока действия")
    boolean expired
) {
}
