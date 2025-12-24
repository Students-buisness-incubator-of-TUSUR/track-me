package net.trackme.sso.components.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.trackme.sso.components.RegistrationStore;
import net.trackme.sso.dto.RecoveryPasswordRequestDto;
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
  private <T> void save(T dto, String sessionId) {
    var json = objectMapper.writeValueAsString(dto);
    store.set(SESSION_ID_TO_REG_DATA + sessionId, json, expireAfter);
  }

  @SneakyThrows
  private <T> Optional<T> take(String sessionId,  Class<T> type) {
    var json = store.get(SESSION_ID_TO_REG_DATA + sessionId);
    if (json == null) {
      return Optional.empty();
    }

    redisTemplate.delete(sessionId);
    return Optional.of(objectMapper.readValue(json, type));
  }

  @SneakyThrows
  @Override
  public void saveToRegistration(RegistrationRequestDto dto, String sessionId) {
    save(dto, sessionId);
  }

  @SneakyThrows
  @Override
  public Optional<RegistrationRequestDto> takeToRegistration(String sessionId) {
    return take(sessionId, RegistrationRequestDto.class);
  }

  @SneakyThrows
  @Override
  public void saveToRecovery(RecoveryPasswordRequestDto dto, String sessionId) {
    save(dto, sessionId);
  }

  @SneakyThrows
  @Override
  public Optional<RecoveryPasswordRequestDto> takeToRecovery(String sessionId) {
    return take(sessionId, RecoveryPasswordRequestDto.class);
  }
}
