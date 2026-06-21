package net.trackme.backend.messaging;

import lombok.Builder;

@Builder
public record MeetingNotHappenedEvent(
        String teamCardUsername,
        String teamCardName,
        String streamName,
        String meetingLink) {
}
