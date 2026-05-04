package net.trackme.meetingservice.messaging;

import net.trackme.meetingservice.messaging.own.MeetingCreatedEvent;
import net.trackme.meetingservice.messaging.own.MeetingEventsProducer;
import net.trackme.meetingservice.messaging.own.MeetingSummaryEvent;
import net.trackme.meetingservice.messaging.own.MeetingUpdatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MeetingEventsProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private MeetingEventsProducer producer;

    @Captor
    private ArgumentCaptor<Message<?>> messageCaptor;

    @Test
    void sendMeetingCreatedEvent_success() {
        UUID meetingId = UUID.randomUUID();
        var event = MeetingCreatedEvent.builder()
                .meetingId(meetingId)
                .teamCardId(UUID.randomUUID())
                .build();

        producer.sendMeetingCreatedEvent(event);

        verify(kafkaTemplate).send(messageCaptor.capture());
        Message<?> captured = messageCaptor.getValue();

        assertEquals(event, captured.getPayload());
        assertEquals("meeting-created", captured.getHeaders().get(KafkaHeaders.TOPIC));
        assertEquals(meetingId.toString(), captured.getHeaders().get(KafkaHeaders.KEY));
    }

    @Test
    void sendMeetingUpdatedEvent_success() {
        UUID meetingId = UUID.randomUUID();
        var event = MeetingUpdatedEvent.builder()
                .meetingId(meetingId)
                .build();

        producer.sendMeetingUpdatedEvent(event);

        verify(kafkaTemplate).send(messageCaptor.capture());
        Message<?> captured = messageCaptor.getValue();

        assertEquals(event, captured.getPayload());
        assertEquals("meeting-updated", captured.getHeaders().get(KafkaHeaders.TOPIC));
        assertEquals(meetingId.toString(), captured.getHeaders().get(KafkaHeaders.KEY));
    }

    @Test
    void sendMeetingSummaryEvents_success() {
        var events = List.of(new MeetingSummaryEvent(
                UUID.randomUUID(),
                "meeting-number",
                "neeting-link")
        );

        producer.sendMeetingSummaryEvents(events);

        verify(kafkaTemplate).send(messageCaptor.capture());
        Message<?> captured = messageCaptor.getValue();

        assertEquals(events, captured.getPayload());
        assertEquals("meeting-summary", captured.getHeaders().get(KafkaHeaders.TOPIC));
        assertNotNull(captured.getHeaders().get(KafkaHeaders.KEY));
    }
}