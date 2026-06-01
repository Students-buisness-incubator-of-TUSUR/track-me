package net.trackme.sso.exception;

/**
 * Исключение, возникающее при ошибке переназначения команд пользователя.
 * Используется при удалении пользователя, когда не удалось переназначить
 * его команды на пользователя Ronin. 
 */
public class TeamReassignmentException extends RuntimeException {
    
    public TeamReassignmentException(String message) {
        super(message);
    }
    
    public TeamReassignmentException(String message, Throwable cause) {
        super(message, cause);
    }
}