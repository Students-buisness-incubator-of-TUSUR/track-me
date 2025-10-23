package net.trackme.backend.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.trackme.backend.services.teamcard.TeamCardMeetingsService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingEventsListener {

    private final TeamCardMeetingsService teamCardMeetingsService;

    @KafkaListener(
            topics = "meeting-created",
            containerFactory = "meetingCreatedListenerContainerFactory")
    public void onMeetingCreatedEvent(
            ConsumerRecord<String, MeetingCreatedEvent> record) {
        var meetingCreatedEvent = record.value();
        log.info("Received meeting created event: {}", meetingCreatedEvent);
        teamCardMeetingsService.increaseMeetingCount(
                meetingCreatedEvent.teamCardId(),
                meetingCreatedEvent.meetingId());
    }

    @KafkaListener(
            topics = "meeting-updated",
            containerFactory = "meetingUpdatedListenerContainerFactory")
    public void onMeetingUpdatedEvent(
            ConsumerRecord<String, MeetingUpdatedEvent> record) {
        var meetingUpdatedEvent = record.value();
        log.info("Received meeting updated event: {}", meetingUpdatedEvent);
        teamCardMeetingsService.updateTeamCardInfo(
                meetingUpdatedEvent.teamCardId(),
                meetingUpdatedEvent.meetingId(),
                meetingUpdatedEvent.newStatus(),
                meetingUpdatedEvent.oldStatus(),
                meetingUpdatedEvent.teamStatus(),
                meetingUpdatedEvent.teamGrade(),
                meetingUpdatedEvent.meetingLink());
    }
}
