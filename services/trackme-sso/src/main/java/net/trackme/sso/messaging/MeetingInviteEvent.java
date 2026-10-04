package net.trackme.sso.messaging;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MeetingInviteEvent(
        UUID meetingId,
        String teamName,
        String trackerUsername,
        String trackerFullName,
        String trackerEmail,
        String creatorUsername,
        OffsetDateTime startDate,
        String meetingLink
) {
}
