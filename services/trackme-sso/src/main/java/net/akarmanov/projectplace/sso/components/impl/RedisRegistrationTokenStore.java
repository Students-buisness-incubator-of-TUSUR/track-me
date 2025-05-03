package net.akarmanov.projectplace.sso.components.impl;

import lombok.extern.slf4j.Slf4j;
import net.akarmanov.projectplace.sso.components.RegistrationTokenStore;
import net.akarmanov.projectplace.sso.dto.RegistrationToken;
import net.akarmanov.projectplace.sso.utils.CryptoUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.apache.commons.lang3.RandomStringUtils.secureStrong;

@Slf4j
public class RedisRegistrationTokenStore implements RegistrationTokenStore {

    public static final Duration TOKEN_EXPIRATION_DURATION = Duration.ofMinutes(10);
    private static final String SESSION_ID_TO_TOKEN = "registration_store:token:";

    private final StringRedisTemplate redisTemplate;
    private final ValueOperations<String, String> store;

    public RedisRegistrationTokenStore(
            StringRedisTemplate stringRedisTemplate,
            ValueOperations<String, String> store) {
        this.redisTemplate = stringRedisTemplate;
        this.store = store;
    }

    @Override
    public RegistrationToken generateToken() {
        String token = secureStrong().nextNumeric(12);
        String tokenHash = CryptoUtils.hash(token);

        log.info("Generate token = {}. Hash = {}", token, tokenHash);

        store.set(SESSION_ID_TO_TOKEN + tokenHash, token, TOKEN_EXPIRATION_DURATION);
        return new RegistrationToken(tokenHash, token);
    }

    @Override
    public boolean isTokenValid(String tokenHash) {
        log.info("Start validate token with tokenHash = {}", tokenHash);
        String storageToken = store.get(SESSION_ID_TO_TOKEN + tokenHash);
        if (storageToken == null) {
            return false;
        }
        redisTemplate.delete(SESSION_ID_TO_TOKEN + tokenHash);
        return true;
    }

}