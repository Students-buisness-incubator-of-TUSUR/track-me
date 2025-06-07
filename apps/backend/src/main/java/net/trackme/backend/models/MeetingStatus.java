package net.trackme.backend.models;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Статус встречи")
public enum MeetingStatus {
  @Schema(description = "Все ок")
  OK(1),
  @Schema(description = "Есть проблемы")
  WITH_ISSUES(0.5),
  @Schema(description = "Много проблем")
  MANY_ISSUES(0.25);

  private final double value;

  MeetingStatus(double value) {
    this.value = value;
  }

}
