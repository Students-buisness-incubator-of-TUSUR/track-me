package net.akarmanov.projectplace.rest.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "DTO создания задачи на встречу")
public record TaskCreateDto(
    @NotBlank(message = "Описание задачи не может быть пустым.")
    @Schema(description = "Описание задачи")
    String description,
    @NotBlank(message = "Ссылка на задачу не может быть пустой.")
    @Schema(description = "Ссылка на задачу")
    String link
) {
}
