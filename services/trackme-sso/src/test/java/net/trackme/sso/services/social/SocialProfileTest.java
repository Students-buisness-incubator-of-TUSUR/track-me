package net.trackme.sso.services.social;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class SocialProfileTest {
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
        assertThrows(IllegalArgumentException.class, () -> SocialProfile.from(attributes));
    }

    @Test
    void acceptsProviderEmailWithoutRequiringName() {
        var profile = SocialProfile.from(Map.of("id", "123", "default_email", "test@yandex.ru"));
        assertEquals("123", profile.id());
        assertEquals("test@yandex.ru", profile.email());
        assertEquals("", profile.name());
        assertTrue(profile.expiresAt().isAfter(Instant.now()));
        assertDoesNotThrow(profile::requireValid);
    }

    @Test
    void limitsNameToDatabaseLength() {
        var profile = SocialProfile.from(Map.of("id", "123", "default_email", "test@yandex.ru", "real_name", "x".repeat(300)));
        assertEquals(255, profile.name().length());
        var expired = new SocialProfile("123", "test@yandex.ru", "", Instant.EPOCH);
        assertThrows(IllegalArgumentException.class, expired::requireValid);
    }
    @Test
    void googleRequiresVerifiedEmailAndKeepsProviderSeparate() {
        var attributes = Map.<String, Object>of("sub", "123", "email", "test@gmail.com", "email_verified", true, "name", "Test");
        var profile = SocialProfile.google(attributes, "+79001234567");
        assertEquals("google", profile.provider());
        assertEquals("+79001234567", profile.phoneNumber());
        var unverified = Map.<String, Object>of("sub", "123", "email", "test@gmail.com", "email_verified", false);
        assertThrows(IllegalArgumentException.class, () -> SocialProfile.google(unverified, ""));
    }
    @Test
    void pendingYandexProfilesSurviveSharedFormMigration() {
        var legacy = new net.trackme.sso.services.yandex.YandexProfile("old123", "person@yandex.ru", "Test", Instant.now().plusSeconds(600));
        var profile = SocialProfile.pending(legacy);
        assertNotNull(profile);
        assertEquals("yandex", profile.provider());
        assertEquals("old123", profile.id());
        assertNull(SocialProfile.pending("invalid"));
    }
    @Test
    void yandexPhoneIsReadFromNestedProviderProfileAndNormalized() {
        var profile = SocialProfile.from(Map.of("id", "123", "default_email", "test@yandex.ru",
                "default_phone", Map.of("number", "+7 (913) 123-45-67")));
        assertEquals("+79131234567", profile.phoneNumber());
        assertEquals("yandex", profile.provider());
    }

    @ParameterizedTest
    @MethodSource("missingOrMalformedPhones")
    void missingOrMalformedYandexPhoneDoesNotBlockRegistration(Object phone) {
        var profile = SocialProfile.from(Map.of("id", "123", "default_email", "test@yandex.ru",
                "default_phone", phone));
        assertEquals("", profile.phoneNumber());
        assertEquals("test@yandex.ru", profile.email());
    }

    static Stream<Object> missingOrMalformedPhones() {
        return Stream.of(Map.of(), "invalid", Map.of("number", 123), Map.of("number", "bad"),
                Map.of("number", "1".repeat(40)));
    }

}
