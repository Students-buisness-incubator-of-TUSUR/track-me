package net.akarmanov.projectplace.rest.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "DTO для рынка НТИ")
public record NTIMarketDto(
        @NotNull(message = "Идентификатор рынка не может быть пустым")
        @Schema(description = "Идентификатор рынка")
        UUID id,
        @Schema(description = "Название рынка")
        String name,
        @Schema(description = "Отображаемое название рынка")
        String displayName
) {
}
