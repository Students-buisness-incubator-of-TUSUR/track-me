package net.akarmanov.projectplace.rest.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

@Schema(description = "DTO для обновления данных пользователя")
@Builder
public record UserUpdateDTO(
    @Schema(description = "Полное имя пользователя",
            example = "Иванов Иван Иванович")
    @NotBlank(message = "Полное имя не может быть пустым")
    String fullName,
    @Schema(description = "Номер телефона в формате +7XXXXXXXXXX",
            example = "+71234567890")
    @Pattern(regexp = "^\\+7\\d{10}$",
             message = "Номер телефона должен быть в формате +7XXXXXXXXXX")
    @NotBlank(message = "Номер телефона не может быть пустым")
    String phoneNumber,
    @NotBlank(message = "Telegram ID не может быть пустым")
    String telegramId,
    @Email
    String email) {
}
