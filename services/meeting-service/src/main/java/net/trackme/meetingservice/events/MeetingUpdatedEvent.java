package net.trackme.meetingservice.events;

import lombok.Builder;
import net.trackme.meetingservice.entities.MeetingStatus;
import net.trackme.meetingservice.entities.TeamStatus;

import java.util.UUID;

@Builder
public record MeetingUpdatedEvent(
        UUID meetingId,
        UUID teamCardId,
        MeetingStatus oldStatus,
        MeetingStatus newStatus,
        TeamStatus teamStatus,
        Double teamGrade,
        String meetingLink) implements MeetingEvent {
}
