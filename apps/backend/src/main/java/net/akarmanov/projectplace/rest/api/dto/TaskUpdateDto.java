package net.akarmanov.projectplace.rest.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TaskUpdateDto(
    @NotNull(message = "Идентификатор задачи не может быть пустым.")
    UUID id,
    String description,
    String status,
    String link
) {
}
