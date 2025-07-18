package net.trackme.backend.rest.api.stream;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "DTO для обновления потока")
public record StreamUpdateDto(
    @Schema(description = "Имя потока")
    @NotBlank(message = "Имя потока не может быть пустым")
    String name,
    @Schema(description = "Дата начала потока")
    @NotNull(message = "Дата начала потока не может быть пустой")
    @Future(message = "Дата начала потока должна быть в будущем")
    LocalDate endDate,
    @Schema(description = "Дата начала трека")
    @NotNull(message = "Дата начала трека не может быть пустой")
    @Future(message = "Дата начала трека должна быть в будущем")
    LocalDate trackStartDate,
    @Schema(description = "Список идентификаторов рынков НТИ")
    @NotNull(message = "Список идентификаторов рынков НТИ не может быть пустым")
    @Size(min = 1, message = "Список идентификаторов рынков НТИ не может быть пустым")
    List<UUID> ntiMarketIds,
    @Schema(description = "Описание потока")
    String description,
    @Schema(description = "Признак активности потока")
    Boolean active,
    @Schema(description = "Количество встреч на команду в потоке")
    Integer meetingsCount
) {
}
