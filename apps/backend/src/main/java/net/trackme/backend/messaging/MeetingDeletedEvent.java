package net.trackme.backend.messaging;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MeetingDeletedEvent(
    UUID meetingId,
    UUID teamCardId,
    OffsetDateTime startDate
) {}