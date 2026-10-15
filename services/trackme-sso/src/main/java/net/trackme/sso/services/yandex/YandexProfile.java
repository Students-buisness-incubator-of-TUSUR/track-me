package net.trackme.sso.services.yandex;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

/** Only provider-verified attributes, kept server-side for ten minutes. */
@lombok.extern.slf4j.Slf4j
public record YandexProfile(String id, String email, String name, Instant expiresAt) implements Serializable {
    public static YandexProfile from(Map<String, Object> attributes) {
        Object subject = attributes.get("id");
        Object mail = attributes.get("default_email");
        if (!(subject instanceof String id) || id.isBlank() || id.length() > 128
                || !(mail instanceof String email) || email.isBlank() || email.length() > 100) {
            // Log only fixed field checks, never identity values, tokens or arbitrary provider keys.
            log.warn("Incomplete Yandex profile: idBlank={}, idTooLong={}, emailBlank={}, emailTooLong={}, emailCount={}",
                    subject instanceof String value && value.isBlank(),
                    subject instanceof String value && value.length() > 128,
                    mail instanceof String value && value.isBlank(),
                    mail instanceof String value && value.length() > 100,
                    attributes.get("emails") instanceof java.util.List<?> emails ? emails.size() : -1);
            throw new IllegalArgumentException("Яндекс не предоставил корректный идентификатор или email. Повторите вход с доступом к почте.");
        }
        Object name = attributes.get("real_name");
        return new YandexProfile(id, email, name instanceof String value ? value.substring(0, Math.min(value.length(), 255)) : "", Instant.now().plusSeconds(600));
    }

    public void requireValid() {
        if (!Instant.now().isBefore(expiresAt)) {
            throw new IllegalArgumentException("Время регистрации истекло. Войдите через Яндекс заново.");
        }
    }
}
