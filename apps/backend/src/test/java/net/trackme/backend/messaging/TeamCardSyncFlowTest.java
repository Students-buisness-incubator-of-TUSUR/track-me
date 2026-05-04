package net.trackme.backend.messaging;

import net.trackme.backend.services.teamcard.TeamCardEventsProducer;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamCardSyncFlowTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private TeamCardEventsProducer producer;

    @Captor
    private ArgumentCaptor<Message<?>> messageCaptor;


    @Test
    void sendTeamCardStreamAddedEvent_success() {
        var teamId = UUID.randomUUID();
        var event = new TeamCardStreamAddedEvent(teamId, UUID.randomUUID());

        producer.sendTeamCardStreamAddedEvent(event);

        verify(kafkaTemplate).send(messageCaptor.capture());
        Message<?> captured = messageCaptor.getValue();

        assertEquals(event, captured.getPayload());
        assertEquals("team-card-stream-added", captured.getHeaders().get(KafkaHeaders.TOPIC));
        assertEquals(teamId.toString(), captured.getHeaders().get(KafkaHeaders.KEY));
    }

    @Test
    void sendTeamCardStreamAddedEvent_nullId_logsError() {
        var event = new TeamCardStreamAddedEvent(null, UUID.randomUUID());

        producer.sendTeamCardStreamAddedEvent(event);

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void sendTeamCardStreamRemovedEvent_success() {
        var teamId = UUID.randomUUID();
        var event = new TeamCardStreamRemovedEvent(teamId, UUID.randomUUID());

        producer.sendTeamCardStreamRemovedEvent(event);

        verify(kafkaTemplate).send(messageCaptor.capture());
        Message<?> captured = messageCaptor.getValue();

        assertEquals("team-card-stream-removed", captured.getHeaders().get(KafkaHeaders.TOPIC));
        assertEquals(teamId.toString(), captured.getHeaders().get(KafkaHeaders.KEY));
    }

    @Test
    void sendTeamCardStreamRemovedEvent_nullId_logsError() {
        var event = new TeamCardStreamRemovedEvent(null, UUID.randomUUID());

        producer.sendTeamCardStreamRemovedEvent(event);

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void sendTeamCardUpdatedEvent_success() {
        var teamId = UUID.randomUUID();
        var event = TeamCardUpdatedEvent.builder()
                .teamCardId(teamId)
                .newName("Name")
                .build();

        producer.sendTeamCardUpdatedEvent(event);

        verify(kafkaTemplate).send(messageCaptor.capture());
        Message<?> captured = messageCaptor.getValue();

        assertEquals("team-card-updated", captured.getHeaders().get(KafkaHeaders.TOPIC));
        assertEquals(teamId.toString(), captured.getHeaders().get(KafkaHeaders.KEY));
    }

    @Test
    void sendTeamCardUpdatedEvent_nullId_logsError() {
        var event = TeamCardUpdatedEvent.builder().teamCardId(null).build();

        producer.sendTeamCardUpdatedEvent(event);

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void sendTeamCardSummaryEvent_success() {
        var events = List.of(new TeamCardSummaryEvent(
                "team-card",
                "stream",
                "meeting-number",
                "meeting-link"
        ));

        producer.sendTeamCardSummaryEvent(events);

        verify(kafkaTemplate).send(messageCaptor.capture());
        Message<?> captured = messageCaptor.getValue();

        assertEquals(events, captured.getPayload());
        assertEquals("team-card-summary", captured.getHeaders().get(KafkaHeaders.TOPIC));
        assertNotNull(captured.getHeaders().get(KafkaHeaders.KEY));
    }

    @Test
    void sendTeamCardLowGradeSummaryEvent_success() {
        var events = List.of(new TeamCardLowGradeSummaryEvent(
                "team-card",
                "stream-name",
                BigDecimal.ONE
        ));

        producer.sendTeamCardLowGradeSummaryEvent(events);

        verify(kafkaTemplate).send(messageCaptor.capture());
        Message<?> captured = messageCaptor.getValue();

        assertEquals(events, captured.getPayload());
        assertEquals("team-card-low-grade-summary", captured.getHeaders().get(KafkaHeaders.TOPIC));
        assertNotNull(captured.getHeaders().get(KafkaHeaders.KEY));
    }
}