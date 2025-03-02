package net.akarmanov.projectplace.rest.api.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;

@Schema(description = "Запрос на сброс пароля")
public record ForgotPasswordRequest(
    @Schema(description = "Email адрес.")
    @Email(message = "Некорректный email")
    String email
) {
}
