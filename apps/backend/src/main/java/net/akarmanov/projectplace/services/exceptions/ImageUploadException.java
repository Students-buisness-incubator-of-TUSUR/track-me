package net.akarmanov.projectplace.services.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class ImageUploadException extends RuntimeException {
  public ImageUploadException(Exception e) {
    super("Ошибка загрузки изображения", e);
  }
}
