package net.trackme.sso.components;

import net.trackme.sso.dto.RegistrationToken;

public interface RegistrationTokenStore {

  RegistrationToken generateToken();

  boolean isTokenValid(String token);

}
