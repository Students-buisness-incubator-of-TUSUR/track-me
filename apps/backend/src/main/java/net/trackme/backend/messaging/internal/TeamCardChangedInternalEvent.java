package net.trackme.backend.messaging.internal;

import java.util.UUID;


public record TeamCardChangedInternalEvent(
    UUID teamCardId,
    String newName,
    String newUsername,
    String trackerFullName
) { }