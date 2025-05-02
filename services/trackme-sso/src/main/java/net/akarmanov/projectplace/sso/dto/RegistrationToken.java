package net.akarmanov.projectplace.sso.dto;

public record RegistrationToken(
        String tokenHash, String token) {
}
