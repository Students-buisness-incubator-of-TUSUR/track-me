package net.trackme.cliengateway.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@org.springframework.boot.autoconfigure.condition.ConditionalOnExpression("${app.yandex.enabled:false} or ${app.google.enabled:false}")
public class YandexConfigurationController {
    private final String ssoUri;

    public YandexConfigurationController(String issuer, String callback) {
        this(issuer, callback, "");
    }

    @org.springframework.beans.factory.annotation.Autowired
    public YandexConfigurationController(@Value("${SSO_URI:http://localhost:9000}") String issuer,
            @Value("${YANDEX_REDIRECT_URI:}") String callback,
            @Value("${GOOGLE_REDIRECT_URI:}") String googleCallback) {
        callback = callback.isBlank() ? googleCallback : callback;
        this.ssoUri = callback.isBlank() ? org.springframework.util.StringUtils.trimTrailingCharacter(issuer, '/')
                : org.springframework.util.StringUtils.trimTrailingCharacter(java.net.URI.create(callback).resolve("/").toString(), '/');
    }

    @GetMapping({"/oauth2/yandex/config", "/oauth2/social/config"})
    public Map<String, String> configuration() {
        return Map.of("ssoUri", ssoUri);
    }
}
