package net.akarmanov.projectplace.sso.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;


@Builder
@Schema(description = "DTO запроса на регистрацию пользователя")
public record RegistrationRequestDto(
    @Schema(description = "Имя пользователя", example = "johndoe")
    @NotBlank(message = "Имя пользователя не может быть пустым.")
    @Size(min = 6, message = "Имя пользователя должно быть не менее 6 символов.")
    String username,
    @Schema(description = "Пароль пользователя", example = "password123")
    @NotBlank(message = "Пароль не может быть пустым.")
    @Size(min = 6, message = "Пароль должен быть не менее 6 символов.")
    String password,
    @Schema(description = "Номер телефона", example = "+79001234567")
    @NotBlank(message = "Номер телефона не может быть пустым.")
    @Pattern(
        regexp = "^\\+?\\d{10,15}$",
        message = "Номер телефона должен содержать от 10 до 15 цифр и может начинаться с +."
    )
    String phoneNumber,
    @Schema(description = "Полное имя пользователя", example = "John Doe")
    String fullName,
    @Schema(description = "Email пользователя", example = "john.doe@example.com")
    @NotBlank(message = "Email не может быть пустым.")
    @Email(message = "Email должен быть корректным.")
    String email,
    @NotBlank(message = "Роль не может быть пустой.")
    String role
) {
}
