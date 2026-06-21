package net.trackme.sso.dto;

public record RegistrationToken(
        String tokenHash, String token) {
}
