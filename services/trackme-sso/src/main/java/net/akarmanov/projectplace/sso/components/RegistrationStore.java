package net.akarmanov.projectplace.sso.components;

import com.fasterxml.jackson.core.JsonProcessingException;
import net.akarmanov.projectplace.sso.dto.RegistrationRequestDto;

public interface RegistrationStore {
  void save(RegistrationRequestDto dto, String sessionId) throws JsonProcessingException;

  RegistrationRequestDto take(String sessionId) throws JsonProcessingException;
}
