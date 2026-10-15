package net.trackme.sso.services.yandex;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.trackme.sso.dto.AuthorizedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

class YandexLoginHandlerTest {
    private final YandexAccountService accounts = mock(YandexAccountService.class);
    private final YandexLoginHandler handler = new YandexLoginHandler(accounts);

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void firstVisitKeepsOnlyPendingProfileAndCannotAuthenticate() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var authentication = external();
        SecurityContextHolder.getContext().setAuthentication(authentication);
        request.getSession().setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext());
        when(accounts.login(any())).thenReturn(Optional.empty());
        handler.onAuthenticationSuccess(request, response, authentication);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNull(request.getSession().getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
        assertInstanceOf(YandexProfile.class, request.getSession().getAttribute(YandexLoginHandler.PENDING));
        assertEquals("/client/yandex/complete", response.getRedirectedUrl());
    }

    @Test
    void repeatedLoginUsesLocalPrincipalAndRotatesSession() throws Exception {
        var user = new AuthorizedUser("local-user", "hash", true, true, true, true,
                List.of(new SimpleGrantedAuthority("TRACKER")));
        when(accounts.login(any())).thenReturn(Optional.of(user));
        var request = new MockHttpServletRequest();
        String oldId = request.getSession().getId();
        handler.onAuthenticationSuccess(request, new MockHttpServletResponse(), external());
        assertNotEquals(oldId, request.getSession().getId());
        assertSame(user, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        assertNotNull(request.getSession().getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
        assertNull(request.getSession().getAttribute(YandexLoginHandler.PENDING));
    }

    @Test
    void inactiveAccountLeavesNoAuthenticatedSession() throws Exception {
        when(accounts.login(any())).thenThrow(new IllegalArgumentException("Ожидает активации"));
        var request = new MockHttpServletRequest();
        handler.onAuthenticationSuccess(request, new MockHttpServletResponse(), external());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNull(request.getSession().getAttribute(YandexLoginHandler.PENDING));
    }

    @Test
    void profileRequiresEmailFromProvider() {
        Map<String, Object> attributes = Map.of("id", "123");
        assertThrows(IllegalArgumentException.class, () -> YandexProfile.from(attributes));
    }

    @Test
    void resumesAuthorizationOnLocalPathAndEncodesParameters() throws Exception {
        var request = new MockHttpServletRequest("GET", "/oauth2/authorize");
        request.setServerName("attacker.example");
        request.addParameter("state", "a&b=1\r\nLocation: https://attacker.example");
        request.addParameter("client_id", "track-me-client");
        var response = new MockHttpServletResponse();
        new org.springframework.security.web.savedrequest.HttpSessionRequestCache().saveRequest(request, response);
        var user = new AuthorizedUser("local-user", "hash", true, true, true, true, List.of());
        handler.complete(request, response, user);
        String target = response.getRedirectedUrl();
        assertTrue(target.startsWith("/oauth2/authorize?"));
        assertTrue(target.contains("state=a%26b%3D1%0D%0ALocation%3A+https%3A%2F%2Fattacker.example"));
        assertFalse(target.contains("\r"));
        assertNull(request.getSession().getAttribute("SPRING_SECURITY_SAVED_REQUEST"));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"//attacker.example", "/client/yandex/link", "/oauth2/authorize/../other"})
    void doesNotResumeUnrelatedSavedPaths(String path) throws Exception {
        var request = new MockHttpServletRequest("GET", path);
        var response = new MockHttpServletResponse();
        new org.springframework.security.web.savedrequest.HttpSessionRequestCache().saveRequest(request, response);
        var user = new AuthorizedUser("local-user", "hash", true, true, true, true, List.of());
        handler.complete(request, response, user);
        assertEquals("/client/login", response.getRedirectedUrl());
    }

    private OAuth2AuthenticationToken external() {
        var authority = new SimpleGrantedAuthority("OAUTH2_USER");
        var user = new DefaultOAuth2User(List.of(authority), Map.of("id", "123", "default_email", "test@yandex.ru"), "id");
        return new OAuth2AuthenticationToken(user, List.of(authority), "yandex");
    }
}
