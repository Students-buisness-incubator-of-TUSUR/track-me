package net.akarmanov.projectplace.services.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class TeamCardNotFoundException extends PPNotFoundException {
  public TeamCardNotFoundException(UUID teamCardId) {
    super("Карточка команды с идентификатором " + teamCardId + " не найдена");
  }

  public TeamCardNotFoundException(UUID teamCardId, UUID userId) {
    super("Карточка команды с идентификатором " + teamCardId + " не найдена для пользователя с идентификатором " + userId);
  }
}
