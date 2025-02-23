package net.akarmanov.projectplace.services.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.UUID;

@Builder
@Schema(description = "DTO для фотографии пользователя")
public record UserPhotoDto(@Schema(description = "Идентификатор фотографии пользователя",
                                   example = "123e4567-e89b-12d3-a456-426614174000")
                           UUID id,
                           @Schema(description = "Имя файла фотографии пользователя",
                                   example = "photo.jpg")
                           String fileName) {
}
