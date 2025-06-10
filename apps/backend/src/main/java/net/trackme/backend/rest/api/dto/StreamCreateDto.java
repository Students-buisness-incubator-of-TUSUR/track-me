package net.trackme.backend.rest.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "DTO для создания потока")
public record StreamCreateDto(
    @Schema(description = "Имя потока")
    @NotBlank(message = "Имя потока не может быть пустым")
    String name,
    @Schema(description = "Дата начала потока")
    @FutureOrPresent(message = "Дата начала потока должна быть в будущем или сегодняшним днем")
    LocalDate startDate,
    @Schema(description = "Дата окончания потока")
    @NotNull(message = "Дата окончания потока не может быть пустой")
    @Future(message = "Дата окончания потока должна быть в будущем")
    LocalDate endDate,
    @Schema(description = "Список идентификаторов рынков НТИ")
    @NotNull(message = "Список идентификаторов рынков НТИ не может быть пустым")
    @Size(min = 1, message = "Список идентификаторов рынков НТИ не может быть пустым")
    List<UUID> ntiMarketIds,
    @Schema(description = "Описание потока")
    String description
) {
}
