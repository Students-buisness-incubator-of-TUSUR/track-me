package net.trackme.backend.services.teamcard;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import net.trackme.backend.messaging.TeamCardStreamAddedEvent;
import net.trackme.backend.messaging.TeamCardStreamRemovedEvent;
import net.trackme.backend.messaging.TeamCardUpdatedEvent;
import net.trackme.backend.messaging.internal.TeamCardChangedInternalEvent;
import net.trackme.backend.messaging.internal.TeamCardStreamAddedInternalEvent;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Слушатель событий синхронизации карточек команд.
 * Отправляет события в Kafka после фиксации транзакции.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TeamCardSyncEventListener {

    /**
     * Продюсер событий Kafka.
     */
    private final TeamCardEventsProducer kafkaProducer;

    /**
     * Обрабатывает изменение метаданных карточки команды.
     * Отправляет обновление в Kafka после фиксации транзакции.
     *
     * @param internalEvent внутреннее событие изменения карточки
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMetadataChange(TeamCardChangedInternalEvent internalEvent) {
        log.info(
            "[Sync] Транзакция зафиксирована. "
                + "Отправка обновления метаданных в Kafka для команды: {} "
                + "(название: {}, трекер: {}, fullName: {})",
            internalEvent.teamCardId(),
            internalEvent.newName(),
            internalEvent.newUsername(),
            internalEvent.trackerFullName()
        );

        kafkaProducer.sendTeamCardUpdatedEvent(TeamCardUpdatedEvent.builder()
                .teamCardId(internalEvent.teamCardId())
                .newName(internalEvent.newName())
                .newUsername(internalEvent.newUsername())
                .trackerFullName(internalEvent.trackerFullName())
                .build());
    }

    /**
     * Обрабатывает добавление потока к карточке команды.
     * Отправляет событие в Kafka после фиксации транзакции.
     *
     * @param internalEvent внутреннее событие добавления потока
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStreamAdded(TeamCardStreamAddedInternalEvent internalEvent) {
        log.info(
            "[Sync] Транзакция зафиксирована. "
                + "Отправка события добавления стрима {} команде {} в Kafka",
            internalEvent.streamId(),
            internalEvent.teamCardId()
        );

        kafkaProducer.sendTeamCardStreamAddedEvent(new TeamCardStreamAddedEvent(
                internalEvent.teamCardId(),
                internalEvent.streamId()
        ));
    }

    /**
     * Обрабатывает удаление потока у карточки команды.
     * Отправляет событие в Kafka после фиксации транзакции.
     *
     * @param internalEvent событие удаления потока
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStreamRemoved(TeamCardStreamRemovedEvent internalEvent) {
        log.info(
            "[Sync] Транзакция зафиксирована. "
                + "Отправка события удаления стрима {} у команды {} в Kafka",
            internalEvent.streamId(),
            internalEvent.teamCardId()
        );

        kafkaProducer.sendTeamCardStreamRemovedEvent(new TeamCardStreamRemovedEvent(
                internalEvent.teamCardId(),
                internalEvent.streamId()
        ));
    }
}
