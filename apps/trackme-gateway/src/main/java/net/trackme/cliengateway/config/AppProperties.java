package net.trackme.cliengateway.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @NotBlank String afterLoginUrl,
        @NotBlank String afterLogoutUri,
        @NotBlank String logoutUri,
        CorsProperties cors,
        SessionCookieProperties sessionCookie) {

    public record SessionCookieProperties(
            String sameSite,
            Boolean secure,
            String domain
    ) {
        public SessionCookieProperties {
            if (sameSite == null) sameSite = "Lax";
        }
    }

    public record CorsProperties(
            List<String> allowedOrigins,
            List<String> allowedOriginPatterns,
            @NotNull List<String> allowedMethods,
            @NotNull List<String> allowedHeaders,
            List<String> exposedHeaders,
            @NotNull Boolean allowCredentials
    ) {

    }
}
