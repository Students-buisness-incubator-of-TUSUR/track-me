package net.trackme.sso.messaging;

public record RegistrationRequestedEvent(String username, String fullName, String role) {
}
