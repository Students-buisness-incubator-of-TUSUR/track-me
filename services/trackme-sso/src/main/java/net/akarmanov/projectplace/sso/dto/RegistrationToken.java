package net.akarmanov.projectplace.sso.dto;

public record RegistrationToken(
    String sessionId, String token) {
}
