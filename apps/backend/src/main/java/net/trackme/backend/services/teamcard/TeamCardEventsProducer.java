package net.trackme.backend.services.teamcard;

import lombok.RequiredArgsConstructor;
import net.trackme.backend.messaging.MeetingNotHappenedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TeamCardEventsProducer {
    private static final String MEETING_NOT_HAPPENED_TOPIC = "meeting-not-happened";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendMeetingNotHappenedEvent(MeetingNotHappenedEvent event) {
        var message = MessageBuilder.withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, MEETING_NOT_HAPPENED_TOPIC)
                .setHeader(KafkaHeaders.KEY, event.username())
                .build();
        kafkaTemplate.send(message);
    }
}
