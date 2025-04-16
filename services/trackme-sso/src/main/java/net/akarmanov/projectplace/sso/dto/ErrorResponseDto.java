package net.akarmanov.projectplace.sso.dto;

import lombok.Builder;

@Builder
public record ErrorResponseDto(
    String error,
    String message,
    int status,
    long timestamp
) {
}
