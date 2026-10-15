package net.trackme.cliengateway.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class YandexConfigurationControllerTest {
    @Test
    void publicCallbackOverridesInternalIssuer() {
        var controller = new YandexConfigurationController("http://internal:9000", "https://sso.example/login/oauth2/code/yandex");
        assertEquals("https://sso.example", controller.configuration().get("ssoUri"));
    }

    @Test
    void issuerIsUsedWithoutCallbackAndTrailingSlashesAreRemoved() {
        var controller = new YandexConfigurationController("https://sso.example///", "");
        assertEquals("https://sso.example", controller.configuration().get("ssoUri"));
    }
}
