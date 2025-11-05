package net.trackme.meetingservice.events;

import lombok.Builder;

import java.util.UUID;

@Builder
public record MeetingSummaryEvent(
        UUID teamCardId,
        String meetingNumber,
        String meetingLink) {
}
