package net.trackme.cliengateway.controller;

import java.time.Instant;
import net.trackme.cliengateway.UserActivityWebFilter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.WebSession;

/** Session metadata; activity writes require the gateway's normal CSRF protection. */
@RestController
public class SessionController {
    @GetMapping("/session/status")
    public SessionStatus status(WebSession session) {
        return snapshot(session);
    }

    @PostMapping("/session/activity")
    public SessionStatus activity(WebSession session) {
        return snapshot(session);
    }

    private SessionStatus snapshot(WebSession session) {
        Long lastActivity = session.getAttribute(UserActivityWebFilter.LAST_ACTIVITY);
        return new SessionStatus(lastActivity, Instant.now().toEpochMilli(),
                UserActivityWebFilter.IDLE_TIMEOUT.toMillis());
    }

    public record SessionStatus(long lastActivityAt, long serverTime, long idleTimeoutMs) { }
}
