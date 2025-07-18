package net.trackme.meetingservice.entities;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Статус команды на встрече")
public enum TeamStatus {
    @Schema(description = "Все ок")
    OK(1),
    @Schema(description = "Есть проблемы")
    WITH_ISSUES(0.5),
    @Schema(description = "Много проблем")
    MANY_ISSUES(0.25);

    private final double value;

    TeamStatus(double value) {
        this.value = value;
    }

}
