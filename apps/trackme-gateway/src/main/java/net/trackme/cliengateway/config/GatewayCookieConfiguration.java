package net.trackme.cliengateway.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.session.CookieWebSessionIdResolver;
import org.springframework.web.server.session.WebSessionIdResolver;

@Configuration
@RequiredArgsConstructor
public class GatewayCookieConfiguration {

    private final AppProperties appProperties;

    @Bean
    WebSessionIdResolver webSessionIdResolver() {
        var cookieProperties = appProperties.sessionCookie();
        var resolver = new CookieWebSessionIdResolver();
        // Настроить параметры cookie
        resolver.setCookieName("SESSION");
        resolver.addCookieInitializer(builder -> {
            builder.sameSite(cookieProperties.sameSite());
            builder.secure(cookieProperties.secure());
            builder.path("/");
            builder.httpOnly(true);
            // Добавляем домен для корректной работы с CSRF
            if (cookieProperties.domain() != null && !cookieProperties.domain().isBlank()) {
                builder.domain(cookieProperties.domain());
            }
        });
        return resolver;
    }
}
