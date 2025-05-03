package net.akarmanov.projectplace.sso.components;

import net.akarmanov.projectplace.sso.dto.RegistrationToken;

public interface RegistrationTokenStore {

  RegistrationToken generateToken();

  boolean isTokenValid(String token);

}
