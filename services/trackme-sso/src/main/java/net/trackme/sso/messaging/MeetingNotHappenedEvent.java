package net.trackme.sso.messaging;

public record MeetingNotHappenedEvent(
        String teamCardUsername,
        String teamCardName,
        String streamName,
        String meetingLink) {
}
