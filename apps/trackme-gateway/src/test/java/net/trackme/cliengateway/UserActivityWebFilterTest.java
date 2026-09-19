package net.trackme.cliengateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.mock.web.server.MockWebSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class UserActivityWebFilterTest {
    private static final long START = Instant.parse("2026-01-01T00:00:00Z").toEpochMilli();

    @Test
    void pollingDoesNotCountAsUserActivity() {
        var session = session();
        var chain = chain();
        StepVerifier.create(filterAt(START + 59 * 60000L).filter(
                exchange(session, false), chain)).verifyComplete();
        assertEquals(START, (Long) session.getAttribute(UserActivityWebFilter.LAST_ACTIVITY));
        verify(chain).filter(any());
    }

    @Test
    void expiresExactlyAtOneHourAndDoesNotReviveOnLateActivity() {
        var session = session();
        var chain = chain();
        var exchange = exchange(session, true);
        StepVerifier.create(filterAt(START + 60 * 60000L).filter(exchange, chain)).verifyComplete();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        assertFalse(session.isStarted());
        verify(chain, never()).filter(any());
    }

    @Test
    void activityExtendsSessionPastOriginalHour() {
        var session = session();
        var chain = chain();
        StepVerifier.create(filterAt(START + 50 * 60000L).filter(
                exchange(session, true), chain)).verifyComplete();
        assertEquals(START + 50 * 60000L, (Long) session.getAttribute(UserActivityWebFilter.LAST_ACTIVITY));
        var later = exchange(session, false);
        StepVerifier.create(filterAt(START + 100 * 60000L).filter(later, chain)).verifyComplete();
        assertFalse(session.isExpired());
    }

    @Test
    void anonymousRequestDoesNotStartActivitySession() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/session/status"));
        var chain = chain();
        StepVerifier.create(filterAt(START).filter(exchange, chain)).verifyComplete();
        verify(chain).filter(exchange);
    }

    private UserActivityWebFilter filterAt(long millis) {
        return new UserActivityWebFilter(Clock.fixed(Instant.ofEpochMilli(millis), ZoneOffset.UTC));
    }

    private MockWebSession session() {
        var session = new MockWebSession();
        session.start();
        session.getAttributes().put(UserActivityWebFilter.LAST_ACTIVITY, START);
        return session;
    }

    private org.springframework.web.server.ServerWebExchange exchange(MockWebSession session, boolean activity) {
        var request = activity ? MockServerHttpRequest.post("/session/activity")
                : MockServerHttpRequest.get("/session/status");
        return MockServerWebExchange.builder(request).session(session).build().mutate()
                .principal(Mono.just(new UsernamePasswordAuthenticationToken("user", "unused", List.of())))
                .build();
    }

    private WebFilterChain chain() {
        var chain = mock(WebFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
        return chain;
    }
}
