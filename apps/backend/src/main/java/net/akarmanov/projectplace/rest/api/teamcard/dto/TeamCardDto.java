package net.akarmanov.projectplace.rest.api.teamcard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import net.akarmanov.projectplace.models.TeamCardStatus;

import java.util.UUID;

@Builder
@Schema(description = "DTO для карточки команды")
public record TeamCardDto(
    @Schema(description = "Идентификатор карточки команды",
            example = "123e4567-e89b-12d3-a456-426614174000")
    UUID id,
    @Schema(description = "Название карточки команды",
            example = "Карточка команды")
    String name,
    @Schema(description = "Описание карточки команды",
            example = "Описание карточки команды")
    String description,
    @Schema(description = "Статус карточки команды",
            example = "Все ок")
    TeamCardStatus status,
    @Schema(description = "Идентификатор пользователя",
            example = "123e4567-e89b-12d3-a456-426614174000")
    UUID userId,
    Boolean enabled
) {
}
