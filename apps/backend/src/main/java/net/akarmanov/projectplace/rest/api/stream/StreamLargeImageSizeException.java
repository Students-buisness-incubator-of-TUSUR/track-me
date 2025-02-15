package net.akarmanov.projectplace.rest.api.stream;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class StreamLargeImageSizeException extends RuntimeException {
  public StreamLargeImageSizeException() {
    super("Размер изображения слишком велик.");
  }
}
