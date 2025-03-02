package net.akarmanov.projectplace.rest.api.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Запрос на сброс пароля")
public record NewPasswordRequest(
    @NotBlank(message = "Токен не может быть пустым")
    @Schema(description = "Токен сброса пароля")
    String token,
    @Schema(description = "Новый пароль")
    @NotBlank(message = "Пароль не может быть пустым")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$",
             message = "Пароль должен содержать минимум 8 символов, включая буквы и цифры")
    String password
) {
}
