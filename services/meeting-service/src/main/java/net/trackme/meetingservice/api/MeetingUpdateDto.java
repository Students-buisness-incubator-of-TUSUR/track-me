package net.trackme.meetingservice.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import lombok.Builder;
import net.trackme.meetingservice.entities.MeetingStatus;
import net.trackme.meetingservice.entities.TeamStatus;

import java.time.OffsetDateTime;

@Builder
@Schema(description = "DTO обновления встречи команды")
public record MeetingUpdateDto(
        @Nullable
        @Schema(description = "Ссылка на встречу")
        String link,
        @Schema(description = "Номер встречи")
        String number,
        @Schema(description = "Статус команды на встрече")
        TeamStatus teamStatus,
        @Schema(description = "Задачи на текущую встречу")
        String tasksCurrentMeeting,
        @Schema(description = "Задачи на следующую встречу")
        String tasksNextMeeting,
        @Nullable
        @Schema(description = "Дата начала встречи")
        OffsetDateTime startDate,
        @Nullable
        @Schema(description = "Статус встречи")
        MeetingStatus status
) {
}
