package net.trackme.sso.messaging;

/**
 * Внутреннее событие приложения, сигнализирующее об изменении данных профиля пользователя.
 * Используется в рамках одной JVM для передачи данных
 */
public record UserUpdatedInternalEvent(
        String username,
        String newFullName
) {}