package net.trackme.meetingservice.messaging.sso;

import lombok.Builder;

/**
 * Событие, уведомляющее систему об обновлении профиля пользователя.
 */
@Builder
public record UserUpdatedEvent(
        String username,
        String newFullName
) {}