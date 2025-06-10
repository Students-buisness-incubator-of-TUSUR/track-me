package net.trackme.sso.components;

import net.trackme.sso.dto.RegistrationRequestDto;

import java.util.Optional;

public interface RegistrationStore {
  void save(RegistrationRequestDto dto, String sessionId);

  Optional<RegistrationRequestDto> take(String sessionId);
}
