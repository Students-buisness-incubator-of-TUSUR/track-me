package net.trackme.telegramservice.messaging;

public record MeetingNotHappenedEvent(
        String teamCardUsername,
        String teamCardName,
        String streamName,
        String meetingLink,
        String trackerFullName) {
}
