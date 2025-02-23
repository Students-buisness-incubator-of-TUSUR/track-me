package net.akarmanov.projectplace.rest.api.teamcard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
    UUID ntiMarketId,

    @Schema(description = "Уровень готовности технологии",
            allowableValues = {"0-2", "3-5", "6-8", "9-10"})
    @NotBlank(message = "Уровень готовности технологии не может быть пустым")
    @Pattern(regexp = "0-2|3-5|6-8|9-10",
             message = "Уровень готовности технологии должен быть одним из значений: 0-2, 3-5, 6-8, 9-10")
    String readinessLevel
) {
}
