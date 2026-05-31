package net.trackme.sso.exception;

/**
 * Исключение, возникающее при ошибке переназначения команд пользователя.
 * Используется при удалении пользователя, когда не удалось переназначить
 * его команды на пользователя Ronin.
 */
public class TeamReassignmentException extends RuntimeException {

    /**
     * Создаёт исключение с указанным сообщением.
     *
     * @param message описание ошибки
     */
    public TeamReassignmentException(String message) {
        super(message);
    }

    /**
     * Создаёт исключение с указанным сообщением и причиной.
     *
     * @param message описание ошибки
     * @param cause причина исключения
     */
    public TeamReassignmentException(String message, Throwable cause) {
        super(message, cause);
    }
}
