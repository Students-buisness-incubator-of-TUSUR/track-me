package net.trackme.sso.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
@Schema(description = "Информация о пользователе")
public record UserDto(
    @Schema(description = "Идентификатор пользователя")
    String id,
    @Schema(description = "Имя пользователя")
    String username,
    @Schema(description = "Роли пользователя")
    List<String> roles,
    @Schema(description = "ФИО пользователя")
    String fullName,
    @Schema(description = "Email пользователя")
    String email,
    @Schema(description = "Телефон пользователя")
    String phoneNumber,
    @Schema(description = "URL аватара пользователя")
    String avatarUrl,
    @Schema(description = "Признак блокировки пользователя")
    boolean enabled
) {
}
