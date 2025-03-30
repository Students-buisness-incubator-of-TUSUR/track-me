package net.akarmanov.projectplace.authservice.api;

import java.util.List;

public record UserUpdateDto(
    String username,
    String password,
    List<String> roles
) {
}
