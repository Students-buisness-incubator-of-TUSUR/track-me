package net.akarmanov.projectplace.rest.api.teamcard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import net.akarmanov.projectplace.rest.api.dto.NTIMarketDto;

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
    @Schema(description = "Рынок НТИ", implementation = NTIMarketDto.class)
    NTIMarketDto ntiMarket
) {
}
