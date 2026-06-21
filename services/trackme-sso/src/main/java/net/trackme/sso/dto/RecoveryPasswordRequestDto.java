package net.trackme.sso.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import lombok.Builder;

@Builder
@Schema(description = "DTO запроса на восстановление пароля")
public record RecoveryPasswordRequestDto(
        @Email(message = "Email должен быть корректным.")
        String email
) {
}
