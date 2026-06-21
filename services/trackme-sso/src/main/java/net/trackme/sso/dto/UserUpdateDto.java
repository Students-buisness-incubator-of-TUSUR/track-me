package net.trackme.sso.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

@Builder
@Schema(name = "DTO для обновления информации о пользователе")
public record UserUpdateDto(
    @NotBlank
    String fullName,
    @Email
    @NotBlank
    String email,
    @NotBlank
    @Pattern(regexp = "^\\+?\\d{10,15}$")
    String phoneNumber,
    @NotBlank
    String avatarUrl
) {
}
