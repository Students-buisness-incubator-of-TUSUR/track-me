package net.akarmanov.projectplace.rest.api.dto;

import java.time.LocalDate;
import java.util.UUID;


public record StreamDto(
        UUID id,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        String ntiMarket,
        String readinessLevel
) {
}
