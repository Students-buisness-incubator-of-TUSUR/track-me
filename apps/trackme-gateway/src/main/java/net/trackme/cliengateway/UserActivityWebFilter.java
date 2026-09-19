package net.trackme.cliengateway;

import java.time.Clock;
import java.time.Duration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/** Enforces user inactivity independently of background HTTP traffic. */
public final class UserActivityWebFilter implements WebFilter {
    public static final String LAST_ACTIVITY = "trackme.lastUserActivity";
    public static final Duration IDLE_TIMEOUT = Duration.ofMinutes(60);
    private final Clock clock;

    public UserActivityWebFilter(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return exchange.getPrincipal().ofType(Authentication.class)
                .map(auth -> auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken))
                .defaultIfEmpty(false)
                .flatMap(authenticated -> {
                    if (!authenticated) {
                        return chain.filter(exchange);
                    }
                    return exchange.getSession().flatMap(session -> {
                        long now = clock.millis();
                        Long lastActivity = session.getAttribute(LAST_ACTIVITY);
                        if (lastActivity == null) {
                            lastActivity = session.getCreationTime().toEpochMilli();
                            session.getAttributes().put(LAST_ACTIVITY, lastActivity);
                        }
                        if (now - lastActivity >= IDLE_TIMEOUT.toMillis()) {
                            return session.invalidate().then(Mono.defer(() -> {
                                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                                return exchange.getResponse().setComplete();
                            }));
                        }
                        if (HttpMethod.POST.equals(exchange.getRequest().getMethod())
                                && "/session/activity".equals(exchange.getRequest().getPath().value())) {
                            session.getAttributes().put(LAST_ACTIVITY, now);
                        }
                        return chain.filter(exchange);
                    });
                });
    }
}
