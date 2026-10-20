package net.trackme.meetingservice.messaging.backend;

import lombok.Builder;

import java.util.UUID;

/**
 * Событие удаления карточки команды (из backend).
 * При получении meeting-service удаляет все встречи, связанные с этой командой.
 *
 * @param teamCardId идентификатор удалённой карточки команды
 */
@Builder
public record TeamCardDeletedEvent(
    UUID teamCardId
) { }