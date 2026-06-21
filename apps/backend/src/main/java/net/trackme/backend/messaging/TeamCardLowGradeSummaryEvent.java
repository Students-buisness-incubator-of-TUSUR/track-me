package net.trackme.backend.messaging;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record TeamCardLowGradeSummaryEvent(
        String teamCardName,
        String streamName,
        BigDecimal averageGrade,
        String trackerFullName
) {}