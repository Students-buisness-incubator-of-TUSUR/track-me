package net.trackme.sso.config.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class YandexLoginConfigurationTest {
    @ParameterizedTest
    @CsvSource({"http://localhost:80/,http://localhost", "https://trackme.example:443/,https://trackme.example",
            "http://localhost:3000/,http://localhost:3000", "https://trackme.example,https://trackme.example"})
    void corsMatchesBrowserOriginIncludingStandardPorts(String configured, String origin) {
        assertEquals(origin, YandexLoginConfiguration.frontendOrigin(configured));
    }

    @Test
    void callbackDefaultsToPublicIssuer() {
        var registrations = new YandexLoginConfiguration().yandexClientRegistrations(
                "client", "test-secret", "https://sso.example/", "");
        var registration = registrations.findByRegistrationId("yandex");
        assertEquals("https://sso.example/login/oauth2/code/yandex", registration.getRedirectUri());
        assertEquals(java.util.Set.of("login:info", "login:email"), registration.getScopes());
    }

    @Test
    void explicitCallbackSupportsLocalDockerIssuer() {
        var registrations = new YandexLoginConfiguration().yandexClientRegistrations(
                "client", "test-secret", "http://internal:9000", "http://localhost:9000/login/oauth2/code/yandex");
        assertEquals("http://localhost:9000/login/oauth2/code/yandex",
                registrations.findByRegistrationId("yandex").getRedirectUri());
    }
}
