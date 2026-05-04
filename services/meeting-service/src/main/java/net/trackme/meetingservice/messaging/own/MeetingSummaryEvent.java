package net.trackme.meetingservice.messaging.own;

import lombok.Builder;

import java.util.UUID;

@Builder
public record MeetingSummaryEvent(
        UUID teamCardId,
        String meetingNumber,
        String meetingLink) {
}
