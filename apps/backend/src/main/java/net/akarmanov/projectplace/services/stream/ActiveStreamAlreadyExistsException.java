package net.akarmanov.projectplace.services.stream;

import net.akarmanov.projectplace.services.exceptions.PPNotFoundException;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ResponseStatus(BAD_REQUEST)
public class ActiveStreamAlreadyExistsException extends PPNotFoundException {
  public ActiveStreamAlreadyExistsException() {
    super("Поток уже существует!");
  }
}
