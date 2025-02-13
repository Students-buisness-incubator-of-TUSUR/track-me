package net.akarmanov.projectplace.services.auth;

import net.akarmanov.projectplace.rest.api.auth.JwtAuthenticationResponse;
import net.akarmanov.projectplace.rest.api.dto.SingInRequest;
import net.akarmanov.projectplace.rest.api.dto.SingUpRequest;

public interface AuthenticationService {
  JwtAuthenticationResponse singUp(SingUpRequest singUpRequest);

  JwtAuthenticationResponse singIn(SingInRequest request);
}
