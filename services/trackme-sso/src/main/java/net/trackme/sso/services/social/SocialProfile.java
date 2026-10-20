package net.trackme.sso.services.social;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

/** Only provider-verified attributes, kept server-side for ten minutes. */
@lombok.extern.slf4j.Slf4j
public record SocialProfile(String id, String email, String name, Instant expiresAt, String provider, String phoneNumber) implements Serializable {
    public SocialProfile(String id, String email, String name, Instant expiresAt) {
        this(id, email, name, expiresAt, "yandex", "");
    }

    public static SocialProfile pending(Object value) {
        if (value instanceof SocialProfile profile) return profile;
        if (value instanceof net.trackme.sso.services.yandex.YandexProfile legacy) {
            return new SocialProfile(legacy.id(), legacy.email(), legacy.name(), legacy.expiresAt());
        }
        return null;
    }

    public static SocialProfile google(Map<String, Object> attributes, String phone) {
        if (!(attributes.get("sub") instanceof String id) || id.isBlank() || id.length() > 128
                || !(attributes.get("email") instanceof String email) || email.isBlank() || email.length() > 100
                || !Boolean.TRUE.equals(attributes.get("email_verified"))) {
            throw new IllegalArgumentException("Google не подтвердил идентификатор или адрес почты.");
        }
        String name = attributes.get("name") instanceof String value ? value : "";
        return new SocialProfile(id, email, name.substring(0, Math.min(name.length(), 255)),
                Instant.now().plusSeconds(600), "google", phone == null ? "" : phone);
    }

    public static SocialProfile from(Map<String, Object> attributes) {
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
        return new SocialProfile(id, email, name instanceof String value ? value.substring(0, Math.min(value.length(), 255)) : "", Instant.now().plusSeconds(600));
    }

    public void requireValid() {
        if (!Instant.now().isBefore(expiresAt)) {
            throw new IllegalArgumentException("Время регистрации истекло. Повторите вход через провайдера.");
        }
    }
}
