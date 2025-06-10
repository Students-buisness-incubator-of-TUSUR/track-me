package net.trackme.sso.components.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.trackme.sso.components.RegistrationStore;
import net.trackme.sso.dto.RegistrationRequestDto;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

@RequiredArgsConstructor
public class RedisRegistrationStore implements RegistrationStore {

  public static final String SESSION_ID_TO_REG_DATA = "registration_store:session_id_to_reg_data:";

  private final Duration expireAfter;

  private final StringRedisTemplate redisTemplate;

  private final ValueOperations<String, String> store;

  private final ObjectMapper objectMapper;

  @SneakyThrows
  @Override
  public void save(RegistrationRequestDto dto, String sessionId) {
    var json = objectMapper.writeValueAsString(dto);
    store.set(SESSION_ID_TO_REG_DATA + sessionId, json, expireAfter);
  }

  @SneakyThrows
  @Override
  public Optional<RegistrationRequestDto> take(String sessionId) {
    var json = store.get(SESSION_ID_TO_REG_DATA + sessionId);
    if (json == null) {
      return Optional.empty();
    }

    redisTemplate.delete(sessionId);
    return Optional.of(objectMapper.readValue(json, RegistrationRequestDto.class));
  }
}
