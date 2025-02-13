package net.akarmanov.projectplace.rest.api.dto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Задача на встречу")
public record TaskDto(
    @Operation(description = "Идентификатор задачи")
    UUID id,
    @Operation(description = "Ссылка на задачу")
    String link,
    @Operation(description = "Номер задачи")
    Integer number,
    @Operation(description = "Описание задачи")
    String description,
    @Operation(description = "Статус задачи")
    String status,
    @Operation(description = "Идентификатор встречи")
    UUID meetingId
) {
}
