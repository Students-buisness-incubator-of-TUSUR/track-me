package net.trackme.backend.messaging.internal;

import java.util.UUID;

public record TeamCardStreamRemovedInternalEvent(
    UUID teamCardId,
    UUID streamId
) { }
