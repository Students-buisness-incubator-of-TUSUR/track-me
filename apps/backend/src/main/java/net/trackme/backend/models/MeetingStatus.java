package net.trackme.backend.models;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Статус встречи")
public enum MeetingStatus {
    SCHEDULED("Запланирована"),
    COMPLETED("Завершена"),
    COMPLETED_AS_NOT_HAPPENED("Завершена как не состоявшаяся");

    private final String value;

    MeetingStatus(String value) {
    this.value = value;
  }

}
