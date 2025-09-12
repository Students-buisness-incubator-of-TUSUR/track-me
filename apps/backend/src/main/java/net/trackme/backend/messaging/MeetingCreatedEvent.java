package net.trackme.backend.messaging;

import java.util.UUID;

public record MeetingCreatedEvent(UUID meetingId, UUID teamCardId) {
}
