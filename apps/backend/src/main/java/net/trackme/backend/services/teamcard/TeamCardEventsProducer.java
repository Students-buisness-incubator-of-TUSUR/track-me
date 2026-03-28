package net.trackme.backend.services.teamcard;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.trackme.backend.messaging.*;
import net.trackme.backend.messaging.internal.TeamCardStreamAddedInternalEvent;
import net.trackme.backend.messaging.internal.TeamCardStreamRemovedInternalEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TeamCardEventsProducer {
    private static final String MEETING_NOT_HAPPENED_TOPIC = "meeting-not-happened";
    private static final String TEAM_CARD_UPDATED_TOPIC = "team-card-updated";
    private static final String TEAM_CARD_SUMMARY_TOPIC = "team-card-summary";
    private static final String TEAM_CARD_STREAM_ADDED_TOPIC = "team-card-stream-added";
    private static final String TEAM_CARD_STREAM_REMOVED_TOPIC = "team-card-stream-removed";
    private static final String TEAM_CARD_LOW_GRADE_SUMMARY_TOPIC = "team-card-low-grade-summary";


    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendMeetingNotHappenedEvent(MeetingNotHappenedEvent event) {
        var message = MessageBuilder.withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, MEETING_NOT_HAPPENED_TOPIC)
                .setHeader(KafkaHeaders.KEY, event.teamCardUsername())
                .build();
        kafkaTemplate.send(message);
    }

    public void sendTeamCardStreamAddedEvent(TeamCardStreamAddedEvent event) {
        if (event.teamCardId() == null) {
            log.error("Cannot send TeamCardStreamAddedEvent: teamCardId is null");
            return;
        }

        log.debug("Sending TeamCardStreamAddedEvent for teamId: {}, streamId: {}",
                event.teamCardId(), event.streamId());

        var message = MessageBuilder.withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, TEAM_CARD_STREAM_ADDED_TOPIC)
                .setHeader(KafkaHeaders.KEY, event.teamCardId().toString())
                .build();

        kafkaTemplate.send(message);
    }

    public void sendTeamCardStreamRemovedEvent(TeamCardStreamRemovedEvent event) {
        if (event.teamCardId() == null) {
            log.error("Cannot send TeamCardStreamRemovedEvent: teamCardId is null");
            return;
        }

        log.debug("Sending TeamCardStreamRemovedEvent for teamId: {}, streamId: {}",
                event.teamCardId(), event.streamId());

        var message = MessageBuilder.withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, TEAM_CARD_STREAM_REMOVED_TOPIC)
                .setHeader(KafkaHeaders.KEY, event.teamCardId().toString())
                .build();

        kafkaTemplate.send(message);
    }

    public void sendTeamCardUpdatedEvent(TeamCardUpdatedEvent event) {
        if (event.teamCardId() == null) {
            log.error("Cannot send TeamCardUpdatedEvent: teamCardId is null");
            return;
        }

        log.debug("Sending TeamCardUpdatedEvent for teamId: {}. New name: {}, New username: {}",
                event.teamCardId(), event.newName(), event.newUsername());

        var message = MessageBuilder.withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, TEAM_CARD_UPDATED_TOPIC)
                .setHeader(KafkaHeaders.KEY, event.teamCardId().toString())
                .build();

        kafkaTemplate.send(message);
    }

    public void sendTeamCardSummaryEvent(List<TeamCardSummaryEvent> events) {
        var message = MessageBuilder.withPayload(events)
                .setHeader(KafkaHeaders.TOPIC, TEAM_CARD_SUMMARY_TOPIC)
                .setHeader(KafkaHeaders.KEY, String.valueOf(UUID.randomUUID()))
                .build();
        kafkaTemplate.send(message);
    }

    public void sendTeamCardLowGradeSummaryEvent(List<TeamCardLowGradeSummaryEvent> events) {
        var message = MessageBuilder.withPayload(events)
                .setHeader(KafkaHeaders.TOPIC, TEAM_CARD_LOW_GRADE_SUMMARY_TOPIC)
                .setHeader(KafkaHeaders.KEY, String.valueOf(UUID.randomUUID()))
                .build();
        kafkaTemplate.send(message);
    }
}