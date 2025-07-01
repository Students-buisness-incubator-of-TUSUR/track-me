package net.trackme.backend.rest.api.meeting;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Future;
import lombok.Builder;
import net.trackme.backend.models.MeetingStatus;

import java.time.OffsetDateTime;

@Builder
@Schema(description = "DTO создания встречи команды")
public record MeetingCreateDto(
        @Nullable
        @Schema(description = "Ссылка на встречу")
        String link,
        @Schema(description = "Номер встречи")
        String number,
        @Schema(description = "Дата начала встречи")
        @Future(message = "Дата начала встречи должна быть в будущем")
        OffsetDateTime startDate,
        @Schema(description = "Задачи на текущую встречу")
        String tasksCurrentMeeting,
        @Schema(description = "Задачи на следующую встречу")
        String tasksNextMeeting,
        @Schema(description = "Статус встречи")
        MeetingStatus status
) {
}