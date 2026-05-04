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

@Slf4j
@Component
@RequiredArgsConstructor
public class TeamCardSyncEventListener {

    private final TeamCardEventsProducer kafkaProducer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMetadataChange(TeamCardChangedInternalEvent internalEvent) {
        log.info(
            "[Sync] Транзакция зафиксирована. " +
            "Отправка обновления метаданных в Kafka для команды: {} (название: {}, трекер: {})",
            internalEvent.teamCardId(), internalEvent.newName(), internalEvent.newUsername()
        );

        kafkaProducer.sendTeamCardUpdatedEvent(TeamCardUpdatedEvent.builder()
                .teamCardId(internalEvent.teamCardId())
                .newName(internalEvent.newName())
                .newUsername(internalEvent.newUsername())
                .build());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStreamAdded(TeamCardStreamAddedInternalEvent internalEvent) {
        log.info(
            "[Sync] Транзакция зафиксирована. " +
            "Отправка события добавления стрима {} команде {} в Kafka",
            internalEvent.streamId(), internalEvent.teamCardId()
        );

        kafkaProducer.sendTeamCardStreamAddedEvent(new TeamCardStreamAddedEvent(
                internalEvent.teamCardId(),
                internalEvent.streamId()
        ));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStreamRemoved(TeamCardStreamRemovedEvent internalEvent) {
        log.info(
            "[Sync] Транзакция зафиксирована. " +
            "Отправка события удаления стрима {} у команды {} в Kafka",
            internalEvent.streamId(), internalEvent.teamCardId()
        );

        kafkaProducer.sendTeamCardStreamRemovedEvent(new TeamCardStreamRemovedEvent(
                internalEvent.teamCardId(),
                internalEvent.streamId()
        ));
    }
}