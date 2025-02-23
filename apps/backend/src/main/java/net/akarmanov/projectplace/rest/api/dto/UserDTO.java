package net.akarmanov.projectplace.rest.api.dto;

import net.akarmanov.projectplace.models.UserRole;
import net.akarmanov.projectplace.services.user.UserPhotoDto;

import java.util.UUID;

public record UserDTO(
    UUID id,
    String fullName,
    String phoneNumber,
    String telegramId,
    String email,
    UserRole role,
    boolean enabled) {
}
