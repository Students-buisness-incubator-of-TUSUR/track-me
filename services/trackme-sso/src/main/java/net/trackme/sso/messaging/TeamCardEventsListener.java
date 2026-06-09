package net.trackme.sso.messaging;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.trackme.sso.services.NotificationService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class TeamCardEventsListener {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "meeting-not-happened",
            containerFactory = "meetingNotHappenedListenerContainerFactory")
    public void onMeetingNotHappenedEvent(
            ConsumerRecord<String, MeetingNotHappenedEvent> record) {
        var meetingNotHappenedEvent = record.value();
        log.info("Received meeting not happened event: {}", meetingNotHappenedEvent);
        notificationService.sendMeetingNotHappenedNotification(
                meetingNotHappenedEvent.teamCardUsername(),
                meetingNotHappenedEvent.teamCardName(),
                meetingNotHappenedEvent.streamName(),
                meetingNotHappenedEvent.meetingLink(),
                meetingNotHappenedEvent.trackerFullName());
    }

    @KafkaListener(
            topics = "team-card-summary",
            containerFactory = "teamCardSummaryListenerContainerFactory")
    public void onTeamCardSummaryEvent(
            ConsumerRecord<String, List<LinkedHashMap<String, String>>> record) {
        var teamCardSummaryEvents = record.value();
        log.info("Received team card summary event: {}", teamCardSummaryEvents);
        notificationService.sendTeamCardSummary(teamCardSummaryEvents);
    }

    @KafkaListener(
            topics = "team-card-low-grade-summary",
            containerFactory = "teamCardLowGradeSummaryListenerContainerFactory")
    public void onTeamCardLowGradeSummaryEvent(
            ConsumerRecord<String, List<LinkedHashMap<String, String>>> record) {
        var teamCardLowGradeSummaryEvents = record.value();
        log.info("Received team card low grade summary event: {}", teamCardLowGradeSummaryEvents);
        notificationService.sendTeamCardLowGradeSummary(teamCardLowGradeSummaryEvents);
    }

    @KafkaListener(
            topics = "meeting-invite",
            containerFactory = "meetingInviteListenerContainerFactory")
    public void onMeetingInviteEvent(ConsumerRecord<String, MeetingInviteEvent> record) {
        var event = record.value();
        log.info("Received meeting invite event for meeting {}", event.meetingId());

        // Отправляем приглашение тимлиду (TrackerUsername = team card owner = тимлид)
        if (event.trackerEmail() != null && !event.trackerEmail().isBlank()) {
            notificationService.sendMeetingInvite(
                    event.trackerEmail(),
                    event.trackerFullName() != null ? event.trackerFullName() : event.trackerUsername(),
                    event.teamName(),
                    event.meetingLink(),
                    event.startDate());
        } else {
            log.warn("No tracker email for meeting {}, skipping tracker invite", event.meetingId());
        }

        // Отправляем приглашение трекеру (создателю встречи), если он отличается от тимлида
        if (event.creatorUsername() != null
                && !event.creatorUsername().isBlank()
                && !Objects.equals(event.creatorUsername(), event.trackerUsername())) {
            notificationService.sendMeetingInviteByUsername(
                    event.creatorUsername(),
                    event.teamName(),
                    event.meetingLink(),
                    event.startDate());
        }
    }

    @KafkaListener(
            topics = "meeting-reminder",
            containerFactory = "meetingReminderListenerContainerFactory")
    public void onMeetingReminderEvent(ConsumerRecord<String, MeetingReminderEvent> record) {
        var event = record.value();
        log.info("Received meeting reminder ({} days) for meeting {}",
                event.daysUntilMeeting(), event.meetingId());

        if (event.trackerUsername() != null && !event.trackerUsername().isBlank()) {
            notificationService.sendMeetingReminderByUsername(
                    event.trackerUsername(),
                    event.teamName(),
                    event.meetingLink(),
                    event.startDate(),
                    event.daysUntilMeeting());
        } else {
            log.warn("No trackerUsername for meeting {}, skipping reminder", event.meetingId());
        }
    }
}
