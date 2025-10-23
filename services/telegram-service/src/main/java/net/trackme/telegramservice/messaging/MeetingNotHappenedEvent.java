package net.trackme.telegramservice.messaging;

import java.math.BigDecimal;

public record MeetingNotHappenedEvent(
        String username,
        String meetingLink,
        BigDecimal averageGrade) {
}
