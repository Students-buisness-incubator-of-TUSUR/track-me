package net.akarmanov.projectplace.services.user;

import lombok.Builder;

import java.util.UUID;

@Builder
public record UserPhotoDto(
    UUID id,
    String fileName,
    byte[] photo
) {
}
