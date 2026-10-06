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
        assertThrows(IllegalArgumentException.class, () -> YandexProfile.from(Map.of("id", "123")));
    }

    private OAuth2AuthenticationToken external() {
        var authority = new SimpleGrantedAuthority("OAUTH2_USER");
        var user = new DefaultOAuth2User(List.of(authority), Map.of("id", "123", "default_email", "test@yandex.ru"), "id");
        return new OAuth2AuthenticationToken(user, List.of(authority), "yandex");
    }
}
