package net.trackme.meetingservice.messaging.own;

import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
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
