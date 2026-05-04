package net.trackme.meetingservice.messaging.backend;

import lombok.Builder;
import java.util.UUID;

/**
 * Событие удаления команды из потока.
 */
@Builder
public record TeamCardStreamRemovedEvent(
        UUID teamCardId,
        UUID streamId
) {}