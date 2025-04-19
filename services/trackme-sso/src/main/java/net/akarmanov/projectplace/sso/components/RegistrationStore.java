package net.akarmanov.projectplace.sso.components;

import net.akarmanov.projectplace.sso.dto.RegistrationRequestDto;

import java.util.Optional;

public interface RegistrationStore {
  void save(RegistrationRequestDto dto, String sessionId);

  Optional<RegistrationRequestDto> take(String sessionId);
}
