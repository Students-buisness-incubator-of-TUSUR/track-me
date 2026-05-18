package net.trackme.meetingservice.messaging.backend;

import lombok.Builder;
import java.util.UUID;

/**
 * Событие обновления основных данных карточки команды.
 *
 * @param teamCardId идентификатор карточки команды
 * @param newName новое название команды
 * @param newUsername новое имя пользователя трекера
 * @param trackerFullName полное имя трекера
 */
@Builder
public record TeamCardUpdatedEvent(
        UUID teamCardId,
        String newName,
        String newUsername,
        String trackerFullName
) { }
