package net.trackme.backend.services.teamcard;

import lombok.RequiredArgsConstructor;
import net.trackme.backend.messaging.TeamCardStreamAddedEvent;
import net.trackme.backend.messaging.TeamCardStreamRemovedEvent;
import net.trackme.backend.messaging.TeamCardUpdatedEvent;
import net.trackme.backend.messaging.internal.TeamCardChangedInternalEvent;
import net.trackme.backend.messaging.internal.TeamCardStreamAddedInternalEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class TeamCardSyncEventListener {

    private final TeamCardEventsProducer kafkaProducer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMetadataChange(TeamCardChangedInternalEvent internalEvent) {
        kafkaProducer.sendTeamCardUpdatedEvent(TeamCardUpdatedEvent.builder()
                .teamCardId(internalEvent.teamCardId())
                .newName(internalEvent.newName())
                .newUsername(internalEvent.newUsername())
                .build());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStreamAdded(TeamCardStreamAddedInternalEvent internalEvent) {
        kafkaProducer.sendTeamCardStreamAddedEvent(new TeamCardStreamAddedEvent(
                internalEvent.teamCardId(),
                internalEvent.streamId()
        ));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStreamRemoved(TeamCardStreamRemovedEvent internalEvent) {
        kafkaProducer.sendTeamCardStreamRemovedEvent(new TeamCardStreamRemovedEvent(
                internalEvent.teamCardId(),
                internalEvent.streamId()
        ));
    }
}