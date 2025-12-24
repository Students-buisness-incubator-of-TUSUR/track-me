package net.trackme.sso.components;

import net.trackme.sso.dto.RecoveryPasswordRequestDto;
import net.trackme.sso.dto.RegistrationRequestDto;

import java.util.Optional;

public interface RegistrationStore {
  void saveToRegistration(RegistrationRequestDto dto, String sessionId);

  Optional<RegistrationRequestDto> takeToRegistration(String sessionId);

  void saveToRecovery(RecoveryPasswordRequestDto dto, String sessionId);

  Optional<RecoveryPasswordRequestDto> takeToRecovery(String sessionId);

}
