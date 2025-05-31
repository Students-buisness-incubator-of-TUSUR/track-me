package net.akarmanov.projectplace.rest.api.teamcard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import net.akarmanov.projectplace.models.TeamCardStatus;
import net.akarmanov.projectplace.rest.api.dto.NTIMarketDto;

import java.math.BigDecimal;
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
    @Schema(description = "Имя пользователя, которому принадлежит карточка команды",
            example = "ivanov")
    String username,
    @Schema(description = "Включена ли карточка команды",
            example = "true", defaultValue = "true")
    Boolean enabled,

    @Schema(description = "Рынок НТИ", implementation = NTIMarketDto.class)
    NTIMarketDto ntiMarket,

    @Schema(description = "Уровень готовности технологии",
            allowableValues = {"0-2", "3-5", "6-8", "9-10"})
    String readinessLevel,

    @Schema(description = "Средняя оценка команды",
            example = "4.5")
    BigDecimal averageGrade
) {
}
