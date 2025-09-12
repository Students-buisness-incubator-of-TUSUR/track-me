package net.trackme.meetingservice.services;

import lombok.RequiredArgsConstructor;
import net.trackme.meetingservice.events.MeetingCreatedEvent;
import net.trackme.meetingservice.events.MeetingUpdatedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MeetingEventsProducer {
    private static final String MEETING_CREATED_TOPIC = "meeting-created";
    private static final String MEETING_UPDATED_TOPIC = "meeting-updated";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendMeetingCreatedEvent(MeetingCreatedEvent event) {
        var message = MessageBuilder.withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, MEETING_CREATED_TOPIC)
                .setHeader(KafkaHeaders.KEY, event.meetingId().toString())
                .build();
        kafkaTemplate.send(message);
    }

    public void sendMeetingUpdatedEvent(MeetingUpdatedEvent event) {
        var message = MessageBuilder.withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, MEETING_UPDATED_TOPIC)
                .setHeader(KafkaHeaders.KEY, event.meetingId().toString())
                .build();
        kafkaTemplate.send(message);
    }
}
