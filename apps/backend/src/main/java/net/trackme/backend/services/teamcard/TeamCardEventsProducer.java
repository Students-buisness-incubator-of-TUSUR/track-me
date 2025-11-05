package net.trackme.backend.services.teamcard;

import lombok.RequiredArgsConstructor;
import net.trackme.backend.messaging.MeetingNotHappenedEvent;
import net.trackme.backend.messaging.TeamCardLowGradeSummaryEvent;
import net.trackme.backend.messaging.TeamCardSummaryEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeamCardEventsProducer {
    private static final String MEETING_NOT_HAPPENED_TOPIC = "meeting-not-happened";
    private static final String TEAM_CARD_SUMMARY_TOPIC = "team-card-summary";
    private static final String TEAM_CARD_LOW_GRADE_SUMMARY_TOPIC = "team-card-low-grade-summary";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendMeetingNotHappenedEvent(MeetingNotHappenedEvent event) {
        var message = MessageBuilder.withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, MEETING_NOT_HAPPENED_TOPIC)
                .setHeader(KafkaHeaders.KEY, event.teamCardUsername())
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