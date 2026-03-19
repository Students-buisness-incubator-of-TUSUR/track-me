package net.trackme.meetingservice.services.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;

import static org.mockito.Mockito.*;

@ActiveProfiles("test")
public class SecurityPropagationInterceptorTest {

    private final SecurityPropagationInterceptor interceptor = new SecurityPropagationInterceptor();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void intercept_withJwtToken_addsHeader() throws IOException {
        // Arrange
        var request = new MockClientHttpRequest();
        var body = new byte[0];
        var execution = mock(ClientHttpRequestExecution.class);

        var jwt = mock(Jwt.class);
        when(jwt.getTokenValue()).thenReturn("test-token-123");
        var auth = new JwtAuthenticationToken(jwt);
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Act
        interceptor.intercept(request, body, execution);

        // Assert
        Assertions.assertEquals("Bearer test-token-123", request.getHeaders().getFirst("Authorization"));
        verify(execution).execute(any(), any());
    }
}