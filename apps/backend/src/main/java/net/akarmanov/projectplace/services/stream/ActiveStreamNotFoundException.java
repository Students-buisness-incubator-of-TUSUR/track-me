package net.akarmanov.projectplace.services.stream;

import net.akarmanov.projectplace.services.exceptions.PPNotFoundException;

import java.util.UUID;

public class ActiveStreamNotFoundException extends PPNotFoundException {
  public ActiveStreamNotFoundException(UUID id) {
    super("Поток с id " + id + " не найден среди активных потоков!");
  }
}
