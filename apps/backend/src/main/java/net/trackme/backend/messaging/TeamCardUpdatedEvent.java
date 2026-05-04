package net.trackme.backend.messaging;

import lombok.Builder;
import java.util.UUID;

@Builder
public record TeamCardUpdatedEvent(
    UUID teamCardId,
    String newName,
    String newUsername
) {
}