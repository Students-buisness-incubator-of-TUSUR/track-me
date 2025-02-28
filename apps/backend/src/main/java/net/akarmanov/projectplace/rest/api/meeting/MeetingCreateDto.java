package net.akarmanov.projectplace.rest.api.meeting;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
@Schema(description = "DTO создания встречи команды")
public record MeetingCreateDto(
    @Schema(description = "Ссылка на встречу")
    @Pattern(regexp = "^(http|https)://.*$",
             message = "Ссылка на встречу должна начинаться с http:// или https://")
    @NotBlank(message = "Ссылка на встречу не может быть пустой")
    String link,
    @Schema(description = "Номер встречи")
    @NotBlank(message = "Номер встречи не может быть пустым")
    String number,
    @Schema(description = "Дата начала встречи")
    @Future(message = "Дата начала встречи должна быть в будущем")
    OffsetDateTime startDate,
    @Schema(description = "Задачи на текущую встречу")
    String tasksCurrentMeeting,
    @Schema(description = "Задачи на следующую встречу")
    String tasksNextMeeting
) {
}
