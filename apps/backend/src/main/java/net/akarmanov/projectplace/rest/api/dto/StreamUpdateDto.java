package net.akarmanov.projectplace.rest.api.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import net.akarmanov.projectplace.domain.NTIMarket;
import net.akarmanov.projectplace.domain.ReadinessLevel;

import java.time.LocalDate;

public record StreamUpdateDto(
        @NotBlank
        String name,
        @NotNull
        @Future
        LocalDate endDate,
        ReadinessLevel readinessLevel,
        NTIMarket ntiMarket
) {
}
