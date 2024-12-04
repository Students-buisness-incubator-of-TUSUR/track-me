package net.akarmanov.projectplace.rest.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import net.akarmanov.projectplace.domain.NTIMarket;
import net.akarmanov.projectplace.domain.ReadinessLevel;

import java.time.LocalDate;

@Schema
public record StreamCreateDto(
        @NotBlank
        String name,
        @FutureOrPresent
        LocalDate startDate,
        @NotNull
        @Future
        LocalDate endDate,
        @NotBlank
        ReadinessLevel readinessLevel,
        @NotBlank
        NTIMarket ntiMarket
) {
}
