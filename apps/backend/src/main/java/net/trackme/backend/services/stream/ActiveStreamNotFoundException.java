package net.trackme.backend.services.stream;

import net.trackme.backend.services.exceptions.PPNotFoundException;

import java.util.UUID;

public class ActiveStreamNotFoundException extends PPNotFoundException {
  public ActiveStreamNotFoundException(UUID id) {
    super("Поток с id " + id + " не найден среди активных потоков!");
  }
}
