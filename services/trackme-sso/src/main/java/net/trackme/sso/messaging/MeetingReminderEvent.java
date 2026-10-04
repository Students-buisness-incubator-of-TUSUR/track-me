package net.trackme.sso.messaging;

import java.time.OffsetDateTime;
import java.util.UUID;

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
