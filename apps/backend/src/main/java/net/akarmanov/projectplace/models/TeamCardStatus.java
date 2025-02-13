package net.akarmanov.projectplace.models;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Schema(description = "Статус карточки команды")
public enum TeamCardStatus {
  @Schema(description = "Все ок")
  OK("Все ок"),
  @Schema(description = "Есть проблемы")
  HAS_ISSUES("Есть проблемы"),
  @Schema(description = "Есть серьезные проблемы")
  HAS_MAJOR_ISSUES("Есть серьезные проблемы");

  private final String description;
}
