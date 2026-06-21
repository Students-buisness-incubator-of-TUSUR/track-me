package net.trackme.backend.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.trackme.backend.services.teamcard.TeamCardMeetingsService;
import net.trackme.backend.services.teamcard.TeamCardSummaryService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Слушатель событий встреч из Kafka.
 *
 * <p>Отключается через {@code kafka.enabled=false} (например, в тестах), чтобы не
 * создавать контейнеры слушателей и не подключаться к брокеру.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MeetingEventsListener {

    private final TeamCardMeetingsService teamCardMeetingsService;

    private final TeamCardSummaryService teamCardSummaryService;

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

    @KafkaListener(
            topics = "meeting-deleted",
            containerFactory = "meetingDeletedListenerContainerFactory")
    public void onMeetingDeletedEvent(
            ConsumerRecord<String, MeetingDeletedEvent> record) {
        var meetingDeletedEvent = record.value();
        log.info("Received meeting deleted event: {}", meetingDeletedEvent);
        teamCardMeetingsService.handleMeetingDeleted(
                meetingDeletedEvent.teamCardId(),
                meetingDeletedEvent.meetingId());
    }

    @KafkaListener(
            topics = "meeting-summary",
            containerFactory = "meetingSummaryListenerContainerFactory")
    public void onMeetingSummaryEvent(
            ConsumerRecord<String, List<LinkedHashMap<String, String>>> record) {
        var meetingSummaryEvents = record.value();
        log.info("Received meetings summary requested event: {}", meetingSummaryEvents);
        teamCardSummaryService.sendTeamCardsSummary(meetingSummaryEvents);
    }
}
