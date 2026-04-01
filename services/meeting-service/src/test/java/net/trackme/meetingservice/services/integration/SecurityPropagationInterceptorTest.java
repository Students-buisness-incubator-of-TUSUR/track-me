package net.trackme.meetingservice.services.integration;

import net.trackme.meetingservice.services.MeetingDataBackfiller;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.util.List;

import static org.mockito.Mockito.*;

@ActiveProfiles("test")
public class SecurityPropagationInterceptorTest {

    private MeetingDataBackfiller backfiller;
    private SecurityPropagationInterceptor interceptor;

    @BeforeEach
    void setUp() {
        backfiller = mock(MeetingDataBackfiller.class);
        interceptor = new SecurityPropagationInterceptor(backfiller);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void intercept_withAdminRole_callsBackfillerAndAddsHeader() throws IOException {
        // Arrange
        var request = new MockClientHttpRequest();
        var body = new byte[0];
        var execution = mock(ClientHttpRequestExecution.class);
        String tokenValue = "admin-token";

        var jwt = mock(Jwt.class);
        when(jwt.getTokenValue()).thenReturn(tokenValue);

        // Создаем аутентификацию с ролью ADMIN
        var auth = new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Act
        interceptor.intercept(request, body, execution);

        // Assert
        Assertions.assertEquals("Bearer " + tokenValue, request.getHeaders().getFirst("Authorization"));
        verify(backfiller, times(1)).run(tokenValue); // Проверяем, что запуск был
        verify(execution).execute(any(), any());
    }

    @Test
    public void intercept_withUserRole_addsHeaderButNoBackfill() throws IOException {
        // Arrange
        var request = new MockClientHttpRequest();
        var body = new byte[0];
        var execution = mock(ClientHttpRequestExecution.class);
        String tokenValue = "user-token";

        var jwt = mock(Jwt.class);
        when(jwt.getTokenValue()).thenReturn(tokenValue);

        // Роль обычного пользователя (не входит в MIGRATION_ROLES)
        var auth = new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Act
        interceptor.intercept(request, body, execution);

        // Assert
        Assertions.assertEquals("Bearer " + tokenValue, request.getHeaders().getFirst("Authorization"));
        verify(backfiller, never()).run(anyString()); // Проверяем, что запуск НЕ вызывался
        verify(execution).execute(any(), any());
    }

    @Test
    public void intercept_noAuth_doesNothing() throws IOException {
        // Arrange
        var request = new MockClientHttpRequest();
        var body = new byte[0];
        var execution = mock(ClientHttpRequestExecution.class);
        SecurityContextHolder.clearContext();

        // Act
        interceptor.intercept(request, body, execution);

        // Assert
        Assertions.assertNull(request.getHeaders().getFirst("Authorization"));
        verify(backfiller, never()).run(anyString());
        verify(execution).execute(any(), any());
    }
}