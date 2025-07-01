package net.trackme.backend.rest.api.meeting;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import lombok.Builder;
import net.trackme.backend.models.MeetingStatus;

@Builder
@Schema(description = "DTO обновления встречи команды")
public record MeetingUpdateDto(
        @Nullable
        @Schema(description = "Ссылка на встречу")
        String link,
        @Schema(description = "Номер встречи")
        String number,
        @Schema(description = "Статус встречи")
        MeetingStatus status,
        @Schema(description = "Задачи на текущую встречу")
        String tasksCurrentMeeting,
        @Schema(description = "Задачи на следующую встречу")
        String tasksNextMeeting
) {
}
