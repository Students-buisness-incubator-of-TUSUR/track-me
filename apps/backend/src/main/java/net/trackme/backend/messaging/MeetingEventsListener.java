package net.trackme.backend.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.trackme.backend.services.teamcard.TeamCardsService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingEventsListener {

    private final TeamCardsService teamCardsService;

    @KafkaListener(
            topics = "meeting-created",
            containerFactory = "meetingCreatedListenerContainerFactory")
    public void onMeetingCreatedEvent(
            ConsumerRecord<String, MeetingCreatedEvent> record) {
        var meetingCreatedEvent = record.value();
        log.info("Received payload: {}", meetingCreatedEvent);
        teamCardsService.increaseMeetingCount(meetingCreatedEvent.teamCardId());
    }
}
