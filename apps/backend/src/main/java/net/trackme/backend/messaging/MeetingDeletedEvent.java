package net.trackme.backend.messaging;

import net.trackme.backend.models.MeetingStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MeetingDeletedEvent(
    UUID meetingId,
    UUID teamCardId,
    OffsetDateTime startDate,
    MeetingStatus status
) {}