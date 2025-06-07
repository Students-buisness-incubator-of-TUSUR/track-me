package net.trackme.backend.rest.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Запрос на аутентификацию.")
public class SingInRequest {
  @Schema(description = "Telegram ID.")
  @Size(min = 4,
        max = 20,
        message = "Telegram ID от 4 до 20 символов.")
  @NotBlank(message = "Telegram ID не может быть null.")
  private String telegramId;

  @Schema(description = "Пароль.")
  @Size(min = 6,
        max = 20,
        message = "Пароль должен быть от 6 до 20 символов.")
  @NotBlank(message = "Пароль не может быть null.")
  private String password;
}
