package net.akarmanov.projectplace.services.exceptions;

import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class TelegramIdExistsException extends RuntimeException {
  public TelegramIdExistsException(@Size(min = 1) String telegramId) {
    super("Пользователь с telegramId " + telegramId + " уже существует.");
  }
}
