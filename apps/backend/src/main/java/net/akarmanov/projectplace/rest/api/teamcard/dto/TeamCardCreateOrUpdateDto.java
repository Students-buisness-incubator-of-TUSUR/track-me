package net.akarmanov.projectplace.rest.api.teamcard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
@Schema(description = "DTO для создания/обновления карточки команды")
public record TeamCardCreateOrUpdateDto(
    @NotBlank(message = "Название карточки команды не может быть пустым")
    @Schema(description = "Название карточки команды",
            example = "Карточка команды")
    String name,
    @Schema(description = "Описание карточки команды",
            example = "Описание карточки команды")
    String description,
    @Schema(description = "Идентификатор рынка NTI",
            example = "00000000-0000-0000-0000-000000000000")
    @NotNull(message = "Идентификатор рынка NTI не может быть пустым")
    @JsonProperty("nti_market_id")
    UUID ntiMarketId
) {
}
