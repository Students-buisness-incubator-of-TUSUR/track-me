package net.trackme.sso.services.yandex;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class YandexProfileTest {
    static Stream<Map<String, Object>> invalidProfiles() {
        return Stream.of(Map.of(), Map.of("id", 123, "default_email", "test@yandex.ru"),
                Map.of("id", "", "default_email", "test@yandex.ru"),
                Map.of("id", "x".repeat(129), "default_email", "test@yandex.ru"),
                Map.of("id", "123"), Map.of("id", "123", "default_email", 123),
                Map.of("id", "123", "default_email", " "),
                Map.of("id", "123", "default_email", "x".repeat(101)));
    }

    @ParameterizedTest
    @MethodSource("invalidProfiles")
    void rejectsIncompleteProviderIdentity(Map<String, Object> attributes) {
        assertThrows(IllegalArgumentException.class, () -> YandexProfile.from(attributes));
    }

    @Test
    void acceptsProviderEmailWithoutRequiringName() {
        var profile = YandexProfile.from(Map.of("id", "123", "default_email", "test@yandex.ru"));
        assertEquals("123", profile.id());
        assertEquals("test@yandex.ru", profile.email());
        assertEquals("", profile.name());
        assertTrue(profile.expiresAt().isAfter(Instant.now()));
        assertDoesNotThrow(profile::requireValid);
    }

    @Test
    void limitsNameToDatabaseLength() {
        var profile = YandexProfile.from(Map.of("id", "123", "default_email", "test@yandex.ru", "real_name", "x".repeat(300)));
        assertEquals(255, profile.name().length());
        var expired = new YandexProfile("123", "test@yandex.ru", "", Instant.EPOCH);
        assertThrows(IllegalArgumentException.class, expired::requireValid);
    }
}
