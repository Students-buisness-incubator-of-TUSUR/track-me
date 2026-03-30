package net.trackme.meetingservice.services.integration;

import lombok.extern.slf4j.Slf4j;
import net.trackme.meetingservice.services.MeetingDataBackfiller;
import org.springframework.context.annotation.Lazy;
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

@Slf4j
@Component
public class SecurityPropagationInterceptor implements ClientHttpRequestInterceptor {

    private final MeetingDataBackfiller backfiller;
    private static final Set<String> MIGRATION_ROLES = Set.of("ROLE_ADMIN", "ROLE_SUPER_ADMIN");

    public SecurityPropagationInterceptor(@Lazy MeetingDataBackfiller backfiller) {
        this.backfiller = backfiller;
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

            // TODO: выпилить этот костыль, если будет гарантия согласованности данных меж сервисами
            boolean canRunBackfill = jwtAuth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(MIGRATION_ROLES::contains);

            if (canRunBackfill) {
                log.debug("[Backfill] Запуск фонового процесса для пользователя {} с ролями {}",
                        token.getSubject(), jwtAuth.getAuthorities()
                );
                backfiller.run(tokenValue);
            }
            //

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
