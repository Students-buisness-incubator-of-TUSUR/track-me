package net.trackme.backend.messaging;

import lombok.Builder;

import java.util.UUID;

@Builder
public record TeamCardStreamRemovedEvent(
    UUID teamCardId,
    UUID streamId
) {
}
