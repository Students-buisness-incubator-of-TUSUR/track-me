package net.trackme.backend.messaging;

import lombok.Builder;

@Builder
public record TeamCardSummaryEvent(
        String teamCardName,
        String streamName,
        String meetingNumber,
        String meetingLink) {
}
