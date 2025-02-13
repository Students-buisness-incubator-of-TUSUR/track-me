package net.akarmanov.projectplace.services.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class UserNotFoundException extends PPNotFoundException {
  public UserNotFoundException(UUID id) {
    super("Пользователь с ID " + id + " не найден!");
  }

  public UserNotFoundException(String telegramId) {
    super("Пользователь с telegramId " + telegramId + " не найден!");
  }
}
