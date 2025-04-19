package net.akarmanov.projectplace.sso.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;


/**
 * DTO запроса на регистрацию пользователя.
 *
 * @param username имя пользователя.
 * @param password пароль пользователя.
 * @param roles    список ролей пользователя.
 */
public record RegistrationRequestDto(
    @NotBlank(message = "Имя пользователя не может быть пустым.")
    @Size(min = 6, message = "Имя пользователя должно быть не менее 6 символов.")
    String username,
    @NotBlank(message = "Пароль не может быть пустым.")
    @Size(min = 6, message = "Пароль должен быть не менее 6 символов.")
    String password,
    @NotBlank(message = "Номер телефона не может быть пустым.")
    @Pattern(
        regexp = "^\\+?\\d{10,15}$",
        message = "Номер телефона должен содержать от 10 до 15 цифр и может начинаться с +."
    )
    String phoneNumber,
    String fullName,
    @NotBlank(message = "Email не может быть пустым.")
    @Email(message = "Email должен быть корректным.")
    String email,
    @NotBlank(message = "Роль не может быть пустой.")
    String role
) {
}
