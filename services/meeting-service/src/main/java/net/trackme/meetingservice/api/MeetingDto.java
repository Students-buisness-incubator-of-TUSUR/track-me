package net.trackme.meetingservice.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import net.trackme.meetingservice.entities.MeetingStatus;
import net.trackme.meetingservice.entities.TeamStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "DTO встречи команды")
public record MeetingDto(
        @Schema(description = "Идентификатор встречи")
        UUID id,

        @Nullable
        @Schema(description = "Ссылка на запись встречи")
        String recordLink,

        @Nullable
        @Schema(description = "Ссылка на комнату встречи")
        String roomLink,

        @Schema(description = "Номер встречи")
        String number,

        @Schema(description = "Дата начала встречи")
        OffsetDateTime startDate,

        @Schema(description = "Статус команды на встрече")
        TeamStatus teamStatus,

        @Schema(description = "Статус встречи")
        MeetingStatus status,

        @Schema(description = "Идентификатор карточки команды")
        String teamCardId,

        @Schema(description = "Текущие задачи на встрече")
        String tasksCurrentMeeting,

        @Schema(description = "Задачи на следующей встрече")
        String tasksNextMeeting
) {
}
