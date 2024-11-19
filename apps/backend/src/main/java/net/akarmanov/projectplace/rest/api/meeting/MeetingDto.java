package net.akarmanov.projectplace.rest.api.meeting;

import io.swagger.v3.oas.annotations.media.Schema;
import net.akarmanov.projectplace.models.MeetingStatus;
import net.akarmanov.projectplace.rest.api.dto.TaskDto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "DTO встречи команды")
public record MeetingDto(
        @Schema(description = "Идентификатор встречи")
        UUID id,
        @Schema(description = "Ссылка на встречу")
        String link,
        @Schema(description = "Номер встречи")
        String number,
        @Schema(description = "Дата начала встречи")
        OffsetDateTime startDate,
        @Schema(description = "Статус встречи")
        MeetingStatus status,
        @Schema(description = "Идентификатор карточки команды")
        String teamCardId,
        @Schema(description = "Список задач на встречу")
        List<TaskDto> tasks
) {
}
