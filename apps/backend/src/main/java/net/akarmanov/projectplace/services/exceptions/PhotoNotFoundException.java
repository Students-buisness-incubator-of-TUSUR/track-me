package net.akarmanov.projectplace.services.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class PhotoNotFoundException extends PPNotFoundException {
  public PhotoNotFoundException(UUID photoId) {
    super("Фотография пользователя с ID " + photoId + " не найдена!");
  }
}
