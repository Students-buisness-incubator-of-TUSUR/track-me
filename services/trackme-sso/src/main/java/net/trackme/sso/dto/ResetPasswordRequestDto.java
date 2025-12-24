package net.trackme.sso.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
@Schema(description = "DTO запроса на сброс пароля")
public record ResetPasswordRequestDto(
    @Schema(description = "Пароль пользователя", example = "password123")
    @NotBlank(message = "Пароль не может быть пустым.")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{6,}$",
            message = "Пароль должен содержать как минимум одну заглавную букву, одну строчную букву, одну цифру и один специальный символ."
    )
    @Size(min = 6, message = "Пароль должен быть не менее 6 символов.")
    String password
) {
}
