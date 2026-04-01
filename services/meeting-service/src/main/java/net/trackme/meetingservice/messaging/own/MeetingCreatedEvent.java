package net.trackme.meetingservice.messaging.own;

import lombok.Builder;

import java.util.UUID;

@Builder
public record MeetingCreatedEvent(
        UUID meetingId,
        UUID teamCardId) implements MeetingEvent {
}
