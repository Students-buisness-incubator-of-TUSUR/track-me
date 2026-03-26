package net.trackme.backend.rest.api.teamcard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import org.hibernate.validator.constraints.URL;

import java.util.List;
import java.util.UUID;

@Builder
@Schema(description = "DTO для создания карточки команды")
public record TeamCardCreateDto(
        @NotBlank(message = "Название карточки команды не может быть пустым")
        @Schema(
                description = "Название карточки команды",
                example = "Карточка команды"
        )
        String name,

        @Schema(
                description = "Описание карточки команды",
                example = "Описание карточки команды"
        )
        String description,

        @NotBlank(message = "Ссылка на комнату для встреч не может быть пустой")
        @URL(message = "Ссылка должна быть корректным URL (например: https://webinar.tusur.ru/b/jm7-p47-8j8-3ib)")
        @Schema(
                description = "Ссылка на комнату для встреч",
                example = "https://webinar.tusur.ru/b/jm7-p47-8j8-3ib"
        )
        String meetingRoomLink,

        @Schema(description = "Идентификаторы рынков НТИ, к которым относится карточка команды")
        List<UUID> ntiMarketIds,

        @NotBlank(message = "Уровень готовности технологии не может быть пустым")
        @Schema(
                description = "Уровень готовности технологии",
                allowableValues = {"0-2", "3-5", "6-8", "9-10"}
        )
        @Pattern(
                regexp = "0-2|3-5|6-8|9-10",
                message = "Уровень готовности технологии должен быть одним из значений: 0-2, 3-5, 6-8, 9-10"
        )
        String readinessLevel
) {
}
