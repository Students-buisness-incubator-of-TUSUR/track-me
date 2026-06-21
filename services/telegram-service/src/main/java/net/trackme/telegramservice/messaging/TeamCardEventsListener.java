package net.trackme.telegramservice.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.trackme.telegramservice.services.NotificationService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

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
        notificationService.sendMeetingNotHappenedMessage(
                meetingNotHappenedEvent.teamCardUsername(),
                meetingNotHappenedEvent.teamCardName(),
                meetingNotHappenedEvent.streamName(),
                meetingNotHappenedEvent.meetingLink(),
                meetingNotHappenedEvent.trackerFullName());
    }
}