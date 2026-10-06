package net.trackme.sso.services.yandex;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

/** Only provider-verified attributes, kept server-side for ten minutes. */
public record YandexProfile(String id, String email, String name, Instant expiresAt) implements Serializable {
    public static YandexProfile from(Map<String, Object> attributes) {
        Object subject = attributes.get("id");
        Object mail = attributes.get("default_email");
        if (!(subject instanceof String id) || id.isBlank() || id.length() > 128
                || !(mail instanceof String email) || email.isBlank() || email.length() > 100) {
            throw new IllegalArgumentException("Яндекс не предоставил идентификатор или email. Разрешите доступ к почте.");
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
