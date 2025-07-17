package net.trackme.cliengateway.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    @NotBlank
    private String afterLoginUrl;

    @NotBlank
    private String afterLogoutUri;

    private List<String> allowedOrigins = new ArrayList<>();

    @NestedConfigurationProperty
    private SessionCookieProperties sessionCookie = new SessionCookieProperties();

    @Setter
    @Getter
    public static class SessionCookieProperties {
        private String sameSite = "Lax";
        private boolean secure = true;
        private String domain;
    }
}
