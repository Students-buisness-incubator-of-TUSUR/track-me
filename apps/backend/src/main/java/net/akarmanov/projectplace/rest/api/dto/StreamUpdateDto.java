package net.akarmanov.projectplace.rest.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "DTO для обновления потока")
public record StreamUpdateDto(
        @Schema(description = "Имя потока")
        @NotBlank(message = "Имя потока не может быть пустым")
        String name,
        @Schema(description = "Дата начала потока")
        @NotNull(message = "Дата начала потока не может быть пустой")
        @Future(message = "Дата начала потока должна быть в будущем")
        LocalDate endDate,
        @Schema(description = "Дата окончания потока")
        String readinessLevel,
        @Size(min = 1, message = "Должен быть выбран хотя бы один рынок НТИ")
        @Schema(description = "Рынки НТИ")
        List<NTIMarketDto> ntiMarkets
) {
}
