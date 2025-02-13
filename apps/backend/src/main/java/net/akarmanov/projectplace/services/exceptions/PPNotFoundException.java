package net.akarmanov.projectplace.services.exceptions;

/**
 * Исключение, которое выбрасывается, если сущность не найдена.
 */
public abstract class PPNotFoundException extends RuntimeException {
  public PPNotFoundException(String message) {
    super(message);
  }
}
