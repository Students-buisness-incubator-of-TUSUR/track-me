package net.akarmanov.projectplace.rest.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import net.akarmanov.projectplace.domain.ReadinessLevel;

import java.time.LocalDate;
import java.util.List;

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
        @Schema(description = "Уровень готовности", implementation = ReadinessLevel.class)
        @NotBlank(message = "Уровень готовности не может быть пустым")
        String readinessLevel,
        @Schema(description = "Рынки НТИ")
        @Size(min = 1, message = "Должен быть выбран хотя бы один рынок НТИ")
        List<NTIMarketDto> ntiMarkets
) {
}
