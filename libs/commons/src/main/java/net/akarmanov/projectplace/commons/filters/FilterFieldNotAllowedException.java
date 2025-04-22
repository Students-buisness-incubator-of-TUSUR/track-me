package net.akarmanov.projectplace.commons.filters;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class FilterFieldNotAllowedException extends RuntimeException {
  public FilterFieldNotAllowedException(
      @NotBlank(message = "Имя поля не может быть пустым") String s, List<String> allowedFields) {
    super("Поле '%s' не разрешено для фильтрации. Разрешенные поля: %s".formatted(s,
        String.join(", ", allowedFields)));
  }

  public FilterFieldNotAllowedException(String message) {
    super("Поле '%s' не разрешено для фильтрации".formatted(message));
  }
}
