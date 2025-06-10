package net.trackme.backend.services.exceptions;

import java.util.UUID;

public class TaskNotFoundException extends PPNotFoundException {
  public TaskNotFoundException(UUID id) {
    super("Задача с id " + id + " не найдена");
  }
}
