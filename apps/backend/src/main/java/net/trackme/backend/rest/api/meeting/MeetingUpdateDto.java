package net.trackme.backend.rest.api.meeting;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import net.trackme.backend.models.MeetingStatus;

@Builder
@Schema(description = "DTO обновления встречи команды")
public record MeetingUpdateDto(
        @Schema(description = "Ссылка на встречу")
        @Pattern(regexp = "^(https?://)?([\\da-z.-]+)\\.([a-z.]{2,6})([/\\w .-]*)*/?$",
                message = "Ссылка должна быть в правильном формате URL",
                flags = Pattern.Flag.CASE_INSENSITIVE)
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
