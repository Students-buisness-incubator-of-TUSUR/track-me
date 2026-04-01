package net.trackme.meetingservice.messaging.backend;


import lombok.Builder;

import java.util.UUID;

/**
 * Событие добавления команды в поток.
 */
@Builder
public record TeamCardStreamAddedEvent(
        UUID teamCardId,
        UUID streamId
) {}
