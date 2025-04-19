package net.akarmanov.projectplace.sso.components;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.akarmanov.projectplace.sso.dto.RegistrationToken;

public interface RegistrationTokenStore {

  RegistrationToken generateToken(HttpServletResponse response);

  boolean isTokenValid(String token, HttpServletRequest request);

  String getSessionId(HttpServletRequest request);
}
