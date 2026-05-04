package net.trackme.meetingservice.services.integration;

import lombok.extern.slf4j.Slf4j;
import net.trackme.meetingservice.services.MeetingDataBackfiller;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class SecurityPropagationInterceptor implements ClientHttpRequestInterceptor {
    private final ObjectProvider<MeetingDataBackfiller> backfillerProvider;
    private final AtomicBoolean backfillCompletedOrDisabled = new AtomicBoolean(false);

    private static final Set<String> MIGRATION_ROLES = Set.of("ROLE_ADMIN", "ROLE_SUPER_ADMIN");

    public SecurityPropagationInterceptor(ObjectProvider<MeetingDataBackfiller> backfillerProvider) {
        this.backfillerProvider = backfillerProvider;
    }

    @NonNull
    @Override
    public ClientHttpResponse intercept(
            @NonNull HttpRequest request,
            @NonNull byte[] body,
            @NonNull ClientHttpRequestExecution execution
    ) throws IOException {

        // --- JWT ---
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            var token = jwtAuth.getToken();
            var tokenValue = token.getTokenValue();

            if (!backfillCompletedOrDisabled.get()) {
                MeetingDataBackfiller bf = backfillerProvider.getIfAvailable();

                if (bf == null) {
                    backfillCompletedOrDisabled.set(true);
                } else {
                    boolean canRunBackfill = jwtAuth.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .anyMatch(MIGRATION_ROLES::contains);

                    if (canRunBackfill) {
                        log.debug("[Backfill] Запуск фонового процесса для пользователя {} с ролями {}",
                                token.getSubject(), jwtAuth.getAuthorities()
                        );
                        bf.run(tokenValue, backfillCompletedOrDisabled);
                    }
                }
            }

            request.getHeaders().setBearerAuth(tokenValue);

            log.debug("[Propagation] JWT поставлено для {} {} | subject={}",
                    request.getMethod(), request.getURI(),
                    token.getSubject());
        } else {
            log.warn("[Propagation] JWT не найден для {} {} | auth={}",
                    request.getMethod(), request.getURI(),
                    authentication == null ? "null" : authentication.getClass().getSimpleName());
        }

        log.debug("[Propagation] Финальные заголовки {} {}: {}",
                request.getMethod(),
                request.getURI(),
                request.getHeaders().keySet()
        );

        return execution.execute(request, body);
    }
}
