package net.trackme.meetingservice.messaging.backend;

import lombok.Builder;
import java.util.UUID;

/**
 * Событие обновления основных данных карточки команды.
 */
@Builder
public record TeamCardUpdatedEvent(
        UUID teamCardId,
        String newName,
        String newUsername
) {}
