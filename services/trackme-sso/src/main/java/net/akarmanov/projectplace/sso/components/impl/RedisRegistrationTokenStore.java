package net.akarmanov.projectplace.sso.components.impl;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import net.akarmanov.projectplace.sso.components.RegistrationTokenStore;
import net.akarmanov.projectplace.sso.dto.RegistrationToken;
import net.akarmanov.projectplace.sso.utils.CryptoUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.codec.Hex;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import static org.apache.commons.lang3.RandomStringUtils.secureStrong;

@Slf4j
public class RedisRegistrationTokenStore implements RegistrationTokenStore {

    private static final String SESSION_ID_TO_TOKEN = "registration_store:token:";

    private final StringRedisTemplate redisTemplate;

    private final ValueOperations<String, String> store;

    private final Duration cookieMaxAge;

    private final String cookieName;

    private final String cookieDomain;

    public RedisRegistrationTokenStore(
        StringRedisTemplate stringRedisTemplate,
        ValueOperations<String, String> store,
        @NotNull Duration cookieMaxAge,
        @NotNull String cookieName,
        @NotNull String cookieDomain) {
        this.redisTemplate = stringRedisTemplate;
        this.store = store;
        this.cookieMaxAge = cookieMaxAge;
        this.cookieName = cookieName;
        this.cookieDomain = cookieDomain;
    }

    @Override
    public RegistrationToken generateToken(HttpServletResponse response) {
        String sessionId = generateSessionId();
        String token = secureStrong().nextNumeric(12);
        String tokenHash = CryptoUtils.hash(sessionId + "-" + token);

        log.info("Generate token = {}. Generate sessionId = {}", token, sessionId);

        store.set(SESSION_ID_TO_TOKEN + sessionId, tokenHash, cookieMaxAge);

        Cookie cookie = buildCookie(sessionId);
        response.addCookie(cookie);

        return new RegistrationToken(sessionId, tokenHash);
    }

    @Override
    public boolean isTokenValid(String token, HttpServletRequest request) {
        String sessionId = getSessionId(request);
        if (sessionId == null) {
            return false;
        }

        log.info("Start validate token with sessionId = {} and token = {}", sessionId, token);

        String storageToken = store.get(SESSION_ID_TO_TOKEN + sessionId);
        if (storageToken == null) {
            return false;
        }

        log.info("Token from storage = {}", storageToken);

        if (storageToken.equals(token)) {
            redisTemplate.delete(SESSION_ID_TO_TOKEN + sessionId);
            return true;
        }
        return false;
    }

    @Override
    public String getSessionId(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (this.cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private Cookie buildCookie(String sessionId) {
        Cookie cookie = new Cookie(cookieName, sessionId);
        cookie.setMaxAge((int) cookieMaxAge.getSeconds());
        cookie.setSecure(true);
        cookie.setHttpOnly(true);
        cookie.setDomain(cookieDomain);
        return cookie;
    }

    private String generateSessionId() {
        UUID uuid = UUID.randomUUID();
        String salt = secureStrong().nextAlphabetic(8);
        byte[] pbkdf = CryptoUtils.pbkdf(
            uuid.toString(),
            salt.getBytes(StandardCharsets.UTF_8),
            256,
            2048
        );
        return new String(Hex.encode(pbkdf));
    }
}