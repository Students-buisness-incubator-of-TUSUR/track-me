package net.akarmanov.projectplace.services.exceptions;

import java.util.UUID;

public class StreamNotFoundException extends PPNotFoundException {
  public StreamNotFoundException(UUID streamId) {
    super("Не найден поток: " + streamId);
  }
}
