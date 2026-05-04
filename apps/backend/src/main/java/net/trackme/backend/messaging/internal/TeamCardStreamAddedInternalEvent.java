package net.trackme.backend.messaging.internal;

import java.util.UUID;

public record TeamCardStreamAddedInternalEvent(
    UUID teamCardId,
    UUID streamId
) { }
