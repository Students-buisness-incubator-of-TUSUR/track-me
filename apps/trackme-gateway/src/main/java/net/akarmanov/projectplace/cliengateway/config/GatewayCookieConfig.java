package net.akarmanov.projectplace.cliengateway.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.session.CookieWebSessionIdResolver;
import org.springframework.web.server.session.WebSessionIdResolver;

@Configuration
@RequiredArgsConstructor
public class GatewayCookieConfig {

    private final AppProperties appProperties;

    @Bean
    WebSessionIdResolver webSessionIdResolver() {
        var cookieProperties = appProperties.getSessionCookie();
        var resolver = new CookieWebSessionIdResolver();
        // Настроить параметры cookie
        resolver.setCookieName("SESSION");
        resolver.addCookieInitializer((builder) -> {
            builder.sameSite(cookieProperties.getSameSite());
            builder.secure(cookieProperties.isSecure());
            builder.path("/");
            builder.httpOnly(true);
        });
        return resolver;

    }
}
