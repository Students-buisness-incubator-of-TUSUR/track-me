package net.trackme.backend.messaging;

import lombok.Builder;

import java.util.UUID;

@Builder
public record TeamCardStreamAddedEvent(
    UUID teamCardId,
    UUID streamId
) {
}
