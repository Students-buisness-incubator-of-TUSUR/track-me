package net.akarmanov.projectplace.rest.api.meeting;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import net.akarmanov.projectplace.models.MeetingStatus;

@Builder
@Schema(description = "DTO обновления встречи команды")
public record MeetingUpdateDto(
    @NotBlank(message = "Ссылка на встречу не может быть пустой")
    @Pattern(regexp = "^(http|https)://.*$",
             message = "Ссылка на встречу должна начинаться с http:// или https://")
    @Schema(description = "Ссылка на встречу")
    String link,
    @NotBlank(message = "Номер встречи не может быть пустым")
    @Schema(description = "Номер встречи")
    String number,
    @NotNull(message = "Статус встречи не может быть пустым")
    @Schema(description = "Статус встречи")
    MeetingStatus status
) {
}
