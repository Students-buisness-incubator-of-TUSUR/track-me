package net.akarmanov.projectplace.services.exceptions;

public class CurrentStreamNotExistsException extends PPNotFoundException {
  public CurrentStreamNotExistsException() {
    super("Нет текущего потока!");
  }
}
