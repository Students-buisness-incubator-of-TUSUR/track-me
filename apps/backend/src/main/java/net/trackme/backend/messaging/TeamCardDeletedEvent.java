package net.trackme.backend.messaging;

import lombok.Builder;

import java.util.UUID;

/**
 * Событие удаления карточки команды.
 * Публикуется в Kafka при физическом удалении TeamCard из backend.
 * Потребители (например, meeting-service) должны удалить связанные данные.
 *
 * @param teamCardId идентификатор удалённой карточки команды
 */
@Builder
public record TeamCardDeletedEvent(
    UUID teamCardId
) { }