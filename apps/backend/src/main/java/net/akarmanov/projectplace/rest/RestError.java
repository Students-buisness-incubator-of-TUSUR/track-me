package net.akarmanov.projectplace.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
@Schema(description = "Ошибка")
public record RestError(
        @Schema(description = "Код ошибки")
        String code,
        @Schema(description = "Сообщение об ошибке")
        String message,
        @Schema(description = "Список ошибок")
        List<String> errors
) {
}
