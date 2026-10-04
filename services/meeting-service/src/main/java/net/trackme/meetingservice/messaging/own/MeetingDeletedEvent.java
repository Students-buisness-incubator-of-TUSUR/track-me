package net.trackme.meetingservice.messaging.own;

import lombok.Builder;
import net.trackme.meetingservice.entities.MeetingStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record MeetingDeletedEvent(
    UUID meetingId,
    UUID teamCardId,
    OffsetDateTime startDate,
    MeetingStatus status
) { }
