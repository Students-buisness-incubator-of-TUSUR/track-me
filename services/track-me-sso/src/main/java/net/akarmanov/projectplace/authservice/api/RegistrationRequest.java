package net.akarmanov.projectplace.authservice.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;


/**
 * DTO запроса на регистрацию пользователя.
 *
 * @param username имя пользователя.
 * @param password пароль пользователя.
 * @param roles    список ролей пользователя.
 */
public record RegistrationRequest(
    @NotBlank(message = "Имя пользователя не может быть пустым.")
    @Min(value = 6, message = "Имя пользователя должно быть не менее 6 символов.")
    String username,
    @NotBlank(message = "Пароль не может быть пустым.")
    @Min(value = 6, message = "Пароль должен быть не менее 6 символов.")
    String password,
    @NotNull(message = "Список ролей не может быть null.")
    @Size(min = 1, message = "Список ролей не может быть пустым.")
    List<String> roles
) {
}
