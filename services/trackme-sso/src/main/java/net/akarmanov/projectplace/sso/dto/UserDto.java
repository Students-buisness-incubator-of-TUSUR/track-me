package net.akarmanov.projectplace.sso.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record UserDto(
    String id,
    String username,
    List<String> roles,
    String fullName,
    String email,
    String phoneNumber,
    String avatarUrl,
    boolean enabled
) {
}
