package net.trackme.meetingservice.messaging.own;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MeetingEventsProducer {
    /**
     * Тема события создания встречи.
     */
    private static final String MEETING_CREATED_TOPIC = "meeting-created";

    /**
     * Тема события обновления встречи.
     */
    private static final String MEETING_UPDATED_TOPIC = "meeting-updated";

    /**
     * Тема события сводки о пропущенных встречах.
     */
    private static final String MEETING_SUMMARY_TOPIC = "meeting-summary";

    /**
     * Шаблон Kafka.
     */
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Отправить событие создания встречи.
     * @param event Событие
     */
    public void sendMeetingCreatedEvent(MeetingCreatedEvent event) {
        var message = MessageBuilder.withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, MEETING_CREATED_TOPIC)
                .setHeader(KafkaHeaders.KEY, event.meetingId().toString())
                .build();
        kafkaTemplate.send(message);
    }

    /**
     * Отправить событие обновления встречи.
     * @param event Событие
     */
    public void sendMeetingUpdatedEvent(MeetingUpdatedEvent event) {
        var message = MessageBuilder.withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, MEETING_UPDATED_TOPIC)
                .setHeader(KafkaHeaders.KEY, event.meetingId().toString())
                .build();
        kafkaTemplate.send(message);
    }

    /**
     * Отпавить событие сводки о пропущенных встречах.
     * @param event События
     */
    public void sendMeetingSummaryEvents(List<MeetingSummaryEvent> event) {
        var message = MessageBuilder.withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, MEETING_SUMMARY_TOPIC)
                .setHeader(KafkaHeaders.KEY, String.valueOf(UUID.randomUUID()))
                .build();
        kafkaTemplate.send(message);
    }
}
