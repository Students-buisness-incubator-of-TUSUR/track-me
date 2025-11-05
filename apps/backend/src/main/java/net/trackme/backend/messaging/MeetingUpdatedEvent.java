package net.trackme.backend.messaging;

import net.trackme.backend.models.MeetingStatus;
import net.trackme.backend.models.TeamCardStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record MeetingUpdatedEvent(
        UUID meetingId,
        UUID teamCardId,
        MeetingStatus oldStatus,
        MeetingStatus newStatus,
        TeamCardStatus teamStatus,
        BigDecimal teamGrade,
        String meetingLink
) {
}
