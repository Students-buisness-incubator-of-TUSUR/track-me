package net.trackme.meetingservice.services;

import lombok.RequiredArgsConstructor;
import net.trackme.meetingservice.events.MeetingCreatedEvent;
import net.trackme.meetingservice.events.MeetingEvent;
import net.trackme.meetingservice.events.MeetingEventType;
import net.trackme.meetingservice.events.MeetingUpdatedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MeetingEventsProducer {
    private static final String TOPIC = "meeting-created";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendMeetingCreatedEvent(MeetingCreatedEvent event) {
        sendMeetingEvent(MeetingEventType.MEETING_CREATED, event);
    }

    public void sendMeetingUpdatedEvent(MeetingUpdatedEvent event) {
        sendMeetingEvent(MeetingEventType.MEETING_UPDATED, event);
    }

    private void sendMeetingEvent(MeetingEventType eventType, MeetingEvent event) {
        var message = MessageBuilder.withPayload(event)
                .setHeader("eventType", eventType.name())
                .setHeader(KafkaHeaders.KEY, event.meetingId().toString())
                .setHeader(KafkaHeaders.TOPIC, TOPIC)
                .build();
        kafkaTemplate.send(message);
    }
}
