package net.trackme.meetingservice.messaging.own;

import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record MeetingReminderEvent(
        UUID meetingId,
        String teamName,
        String trackerUsername,
        String trackerFullName,
        OffsetDateTime startDate,
        String meetingLink,
        int daysUntilMeeting
) {
}
