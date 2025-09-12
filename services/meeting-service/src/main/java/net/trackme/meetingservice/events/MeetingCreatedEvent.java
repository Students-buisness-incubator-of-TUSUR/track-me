package net.trackme.meetingservice.events;

import lombok.Builder;

import java.util.UUID;

@Builder
public record MeetingCreatedEvent(
        UUID meetingId,
        UUID teamCardId) implements MeetingEvent {
}
