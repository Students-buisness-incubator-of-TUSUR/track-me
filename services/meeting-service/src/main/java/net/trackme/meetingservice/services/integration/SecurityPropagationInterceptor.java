package net.trackme.meetingservice.services.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class SecurityPropagationInterceptor implements ClientHttpRequestInterceptor {

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
