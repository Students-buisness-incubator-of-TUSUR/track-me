package net.trackme.backend.services.teamcard;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.trackme.backend.messaging.*;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Сервис-продюсер для публикации событий, связанных с карточками команд, в систему Kafka.
 */
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

    /**
     * Отправляет событие о том, что запланированная встреча команды не состоялась.
     *
     * @param event данные о несостоявшейся встрече.
     */
    public void sendMeetingNotHappenedEvent(MeetingNotHappenedEvent event) {
        log.debug("[Kafka] Отправка события о несостоявшейся встрече для трекера: {}", event.teamCardUsername());
        var message = MessageBuilder.withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, MEETING_NOT_HAPPENED_TOPIC)
                .setHeader(KafkaHeaders.KEY, event.teamCardUsername())
                .build();
        kafkaTemplate.send(message);
    }

    /**
     * Публикует событие о добавлении команды в новый поток (стрим).
     *
     * @param event данные о привязке команды к потоку.
     */
    public void sendTeamCardStreamAddedEvent(TeamCardStreamAddedEvent event) {
        if (event.teamCardId() == null) {
            log.error("[Kafka] Ошибка отправки события: teamCardId равен null");
            return;
        }

        log.debug("[Kafka] Отправка события добавления стрима. Команда: {}, Стрим: {}",
                event.teamCardId(), event.streamId());

        var message = MessageBuilder.withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, TEAM_CARD_STREAM_ADDED_TOPIC)
                .setHeader(KafkaHeaders.KEY, event.teamCardId().toString())
                .build();

        kafkaTemplate.send(message);
    }

    /**
     * Публикует событие об удалении привязки команды к потоку (стриму).
     *
     * @param event данные об исключении команды из потока.
     */
    public void sendTeamCardStreamRemovedEvent(TeamCardStreamRemovedEvent event) {
        if (event.teamCardId() == null) {
            log.error("[Kafka] Ошибка отправки события: teamCardId равен null");
            return;
        }

        log.debug("[Kafka] Отправка события удаления стрима. Команда: {}, Стрим: {}",
                event.teamCardId(), event.streamId());

        var message = MessageBuilder.withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, TEAM_CARD_STREAM_REMOVED_TOPIC)
                .setHeader(KafkaHeaders.KEY, event.teamCardId().toString())
                .build();

        kafkaTemplate.send(message);
    }

    /**
     * Публикует событие изменения основных данных карточки команды (название, ответственный трекер).
     *
     * @param event обновленные данные карточки команды.
     */
    public void sendTeamCardUpdatedEvent(TeamCardUpdatedEvent event) {
        if (event.teamCardId() == null) {
            log.error("[Kafka] Ошибка отправки события обновления: teamCardId равен null");
            return;
        }

        log.debug("[Kafka] Отправка события обновления карточки. Команда: {}, Название: {}, Трекер: {}",
                event.teamCardId(), event.newName(), event.newUsername());

        var message = MessageBuilder.withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, TEAM_CARD_UPDATED_TOPIC)
                .setHeader(KafkaHeaders.KEY, event.teamCardId().toString())
                .build();

        kafkaTemplate.send(message);
    }

    /**
     * Отправляет пакет событий со сводной информацией по карточкам команд.
     *
     * @param events список событий сводки.
     */
    public void sendTeamCardSummaryEvent(List<TeamCardSummaryEvent> events) {
        log.debug("[Kafka] Отправка пакета сводной информации по командам. Размер: {}", events.size());
        var message = MessageBuilder.withPayload(events)
                .setHeader(KafkaHeaders.TOPIC, TEAM_CARD_SUMMARY_TOPIC)
                .setHeader(KafkaHeaders.KEY, String.valueOf(UUID.randomUUID()))
                .build();
        kafkaTemplate.send(message);
    }

    /**
     * Отправляет пакет событий по командам с критически низкими оценками.
     *
     * @param events список событий с информацией о низких оценках.
     */
    public void sendTeamCardLowGradeSummaryEvent(List<TeamCardLowGradeSummaryEvent> events) {
        log.debug("[Kafka] Отправка отчета по низким оценкам. Размер пакета: {}", events.size());
        var message = MessageBuilder.withPayload(events)
                .setHeader(KafkaHeaders.TOPIC, TEAM_CARD_LOW_GRADE_SUMMARY_TOPIC)
                .setHeader(KafkaHeaders.KEY, String.valueOf(UUID.randomUUID()))
                .build();
        kafkaTemplate.send(message);
    }
}