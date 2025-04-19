package net.akarmanov.projectplace.sso.components.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import net.akarmanov.projectplace.sso.components.RegistrationStore;
import net.akarmanov.projectplace.sso.dto.RegistrationRequestDto;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

@RequiredArgsConstructor
public class RedisRegistrationStore implements RegistrationStore {

  private static final String SESSION_ID_TO_REG_DATA = "registration_store:session_id_to_reg_data:";

  private final Duration expireAfter;

  private final StringRedisTemplate redisTemplate;

  private final ValueOperations<String, String> store;

  private final ObjectMapper objectMapper;

  @Override
  public void save(RegistrationRequestDto dto, String sessionId) throws JsonProcessingException {
    var json = objectMapper.writeValueAsString(dto);
    store.set(SESSION_ID_TO_REG_DATA + sessionId, json, expireAfter);
  }

  @Override
  public RegistrationRequestDto take(String sessionId) throws JsonProcessingException {
    var json = store.get(SESSION_ID_TO_REG_DATA + sessionId);
    if (json == null) {
      return null;
    }
    redisTemplate.delete(sessionId);
    return objectMapper.readValue(json, RegistrationRequestDto.class);
  }
}
