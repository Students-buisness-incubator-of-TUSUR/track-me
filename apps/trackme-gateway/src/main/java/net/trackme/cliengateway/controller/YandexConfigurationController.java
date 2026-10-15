package net.trackme.cliengateway.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(name = "app.yandex.enabled", havingValue = "true")
public class YandexConfigurationController {
    private final String ssoUri;

    public YandexConfigurationController(@Value("${SSO_URI:http://localhost:9000}") String issuer,
            @Value("${YANDEX_REDIRECT_URI:}") String callback) {
        this.ssoUri = callback.isBlank() ? org.springframework.util.StringUtils.trimTrailingCharacter(issuer, '/')
                : org.springframework.util.StringUtils.trimTrailingCharacter(java.net.URI.create(callback).resolve("/").toString(), '/');
    }

    @GetMapping("/oauth2/yandex/config")
    public Map<String, String> configuration() {
        return Map.of("ssoUri", ssoUri);
    }
}
