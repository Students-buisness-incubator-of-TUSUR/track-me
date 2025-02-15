package net.akarmanov.projectplace.rest.api.stream;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class StreamEmptyImageException extends RuntimeException {
  public StreamEmptyImageException() {
    super("Изображение не может быть пустым.");
  }
}
