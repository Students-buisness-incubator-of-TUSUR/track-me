package net.akarmanov.projectplace.services.stream;

import net.akarmanov.projectplace.services.exceptions.PPNotFoundException;

public class ActiveStreamNotExistsException extends PPNotFoundException {
  public ActiveStreamNotExistsException() {
    super("Нет текущего потока!");
  }
}
