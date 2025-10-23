package net.trackme.backend.messaging;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record MeetingNotHappenedEvent(
        String username,
        String meetingLink,
        BigDecimal averageGrade) {
}
