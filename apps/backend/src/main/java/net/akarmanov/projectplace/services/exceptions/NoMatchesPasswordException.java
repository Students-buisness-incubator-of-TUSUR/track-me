package net.akarmanov.projectplace.services.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class NoMatchesPasswordException extends RuntimeException {
  public NoMatchesPasswordException() {
    super("Указанный пароль не совпадает с текущим");
  }
}
