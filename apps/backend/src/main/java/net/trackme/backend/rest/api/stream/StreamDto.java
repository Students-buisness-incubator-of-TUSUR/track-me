package net.trackme.backend.rest.api.stream;

import io.swagger.v3.oas.annotations.media.Schema;
import net.trackme.backend.rest.api.dto.NTIMarketDto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "DTO для потока")
public record StreamDto(
    @Schema(description = "Идентификатор потока")
    UUID id,
    @Schema(description = "Имя потока")
    String name,
    @Schema(description = "Дата начала потока")
    LocalDate startDate,
    @Schema(description = "Дата окончания потока")
    LocalDate endDate,
    @Schema(description = "Рынки НТИ")
    List<NTIMarketDto> ntiMarkets,
    @Schema(description = "Описание потока")
    String description,
    @Schema(description = "Признак активности потока")
    Boolean active
) {
}
