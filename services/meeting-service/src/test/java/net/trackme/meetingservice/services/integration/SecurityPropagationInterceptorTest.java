package net.trackme.meetingservice.services.integration;

import net.trackme.meetingservice.services.MeetingDataBackfiller;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.Mockito.*;

public class SecurityPropagationInterceptorTest {

    private MeetingDataBackfiller backfiller;
    private ObjectProvider<MeetingDataBackfiller> provider;
    private SecurityPropagationInterceptor interceptor;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        backfiller = mock(MeetingDataBackfiller.class);
        provider = mock(ObjectProvider.class);

        // По умолчанию провайдер возвращает наш мок
        when(provider.getIfAvailable()).thenReturn(backfiller);

        interceptor = new SecurityPropagationInterceptor(provider);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void intercept_withAdminRole_callsBackfillerWithFlag() throws IOException {
        // Arrange
        var request = new MockClientHttpRequest();
        var body = new byte[0];
        var execution = mock(ClientHttpRequestExecution.class);
        String tokenValue = "admin-token";

        var jwt = mock(Jwt.class);
        when(jwt.getTokenValue()).thenReturn(tokenValue);
        var auth = new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Act
        interceptor.intercept(request, body, execution);

        // Assert
        Assertions.assertEquals("Bearer " + tokenValue, request.getHeaders().getFirst("Authorization"));

        verify(backfiller, times(1)).run(eq(tokenValue), any(AtomicBoolean.class));
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
        var auth = new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Act
        interceptor.intercept(request, body, execution);

        // Assert
        Assertions.assertEquals("Bearer " + tokenValue, request.getHeaders().getFirst("Authorization"));

        verify(backfiller, never()).run(anyString(), any());
        verify(execution).execute(any(), any());
    }

    @Test
    public void intercept_whenBackfillerMissing_doesNotCrash() throws IOException {
        when(provider.getIfAvailable()).thenReturn(null);

        var request = new MockClientHttpRequest();
        var body = new byte[0];
        var execution = mock(ClientHttpRequestExecution.class);

        var jwt = mock(Jwt.class);
        when(jwt.getTokenValue()).thenReturn("token");
        var auth = new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Act & Assert
        Assertions.assertDoesNotThrow(() -> interceptor.intercept(request, body, execution));
        verify(execution).execute(any(), any());
    }

    @Test
    public void intercept_secondCall_skipsProviderLookup() throws IOException {
        // Проверяем работу оптимизации (флаг backfillCompletedOrDisabled)
        var request = new MockClientHttpRequest();
        var body = new byte[0];
        var execution = mock(ClientHttpRequestExecution.class);

        var jwt = mock(Jwt.class);
        when(jwt.getTokenValue()).thenReturn("token");
        var auth = new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        doAnswer(invocation -> {
            AtomicBoolean flag = invocation.getArgument(1);
            flag.set(true);
            return null;
        }).when(backfiller).run(anyString(), any(AtomicBoolean.class));

        interceptor.intercept(request, body, execution);
        interceptor.intercept(request, body, execution);

        verify(provider, times(1)).getIfAvailable();
    }
}