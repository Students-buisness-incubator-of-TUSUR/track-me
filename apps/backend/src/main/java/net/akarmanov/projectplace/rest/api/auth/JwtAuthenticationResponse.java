package net.akarmanov.projectplace.rest.api.auth;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Ответ на аутентификацию.")
public class JwtAuthenticationResponse {
  @Schema(description = "Токен.")
  private String accessToken;

  @Schema(description = "Тип токена.")
  private String tokenType = "Bearer";

  public JwtAuthenticationResponse(String accessToken) {
    this.accessToken = accessToken;
  }
}
