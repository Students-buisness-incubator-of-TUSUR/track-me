package net.trackme.cliengateway.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.server.WebSession;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2ClientConfigurationHelperTest {

    @Mock
    private ReactiveClientRegistrationRepository clientRegistrationRepository;

    private AppProperties appProperties;
    private OAuth2ClientConfiguration configuration;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties(
                "http://localhost:3000/after-login",
                "http://localhost:3000",
                "http://localhost:9000/connect/logout",
                "http://localhost:9000/client/registration",
                new AppProperties.CorsProperties(
                        null,
                        List.of("http://localhost:*"),
                        List.of("GET", "POST"),
                        List.of("Content-Type"),
                        null,
                        true
                ),
                new AppProperties.SessionCookieProperties("Lax", false, "")
        );
        configuration = new OAuth2ClientConfiguration(clientRegistrationRepository, appProperties);
    }

    @Test
    void extractRegistrationId_withOAuth2AuthenticationToken_returnsRegistrationId() throws Exception {
        Method method = OAuth2ClientConfiguration.class
                .getDeclaredMethod("extractRegistrationId", Authentication.class);
        method.setAccessible(true);

        OAuth2AuthenticationToken oauthToken = mock(OAuth2AuthenticationToken.class);
        when(oauthToken.getAuthorizedClientRegistrationId()).thenReturn("google");

        String result = (String) method.invoke(configuration, oauthToken);

        assertEquals("google", result);
    }

    @Test
    void extractRegistrationId_withNonOAuth2Authentication_returnsEmpty() throws Exception {
        Method method = OAuth2ClientConfiguration.class
                .getDeclaredMethod("extractRegistrationId", Authentication.class);
        method.setAccessible(true);

        Authentication authentication = mock(Authentication.class);

        String result = (String) method.invoke(configuration, authentication);

        assertEquals("", result);
    }

    @Test
    void isExternalOAuthProvider_google_returnsTrue() throws Exception {
        Method method = OAuth2ClientConfiguration.class
                .getDeclaredMethod("isExternalOAuthProvider", String.class);
        method.setAccessible(true);

        assertTrue((Boolean) method.invoke(configuration, "google"));
    }

    @Test
    void isExternalOAuthProvider_yandex_returnsTrue() throws Exception {
        Method method = OAuth2ClientConfiguration.class
                .getDeclaredMethod("isExternalOAuthProvider", String.class);
        method.setAccessible(true);

        assertTrue((Boolean) method.invoke(configuration, "yandex"));
    }

    @Test
    void isExternalOAuthProvider_internalProvider_returnsFalse() throws Exception {
        Method method = OAuth2ClientConfiguration.class
                .getDeclaredMethod("isExternalOAuthProvider", String.class);
        method.setAccessible(true);

        assertFalse((Boolean) method.invoke(configuration, "track-me-client"));
    }

    @Test
    void buildOAuthRegistrationUrl_withGoogleAttributes_containsEmailAndName() throws Exception {
        Method method = OAuth2ClientConfiguration.class
                .getDeclaredMethod("buildOAuthRegistrationUrl", OAuth2User.class);
        method.setAccessible(true);

        OAuth2User oauth2User = mock(OAuth2User.class);
        Map<String, Object> attrs = Map.of("sub", "12345", "email", "user@google.com", "name", "Google User");
        when(oauth2User.getAttributes()).thenReturn(attrs);

        String result = (String) method.invoke(configuration, oauth2User);

        assertNotNull(result);
        assertTrue(result.startsWith("http://localhost:9000/client/registration"));
        assertTrue(result.contains("email="));
        assertTrue(result.contains("name="));
    }

    @Test
    void buildOAuthRegistrationUrl_withYandexAttributes_usesDefaultEmail() throws Exception {
        Method method = OAuth2ClientConfiguration.class
                .getDeclaredMethod("buildOAuthRegistrationUrl", OAuth2User.class);
        method.setAccessible(true);

        OAuth2User oauth2User = mock(OAuth2User.class);
        Map<String, Object> attrs = Map.of("default_email", "user@yandex.ru", "real_name", "Yandex User");
        when(oauth2User.getAttributes()).thenReturn(attrs);

        String result = (String) method.invoke(configuration, oauth2User);

        assertNotNull(result);
        assertTrue(result.startsWith("http://localhost:9000/client/registration"));
    }

    @Test
    void buildOAuthRegistrationUrl_withNullEmailAndName_usesEmptyStrings() throws Exception {
        Method method = OAuth2ClientConfiguration.class
                .getDeclaredMethod("buildOAuthRegistrationUrl", OAuth2User.class);
        method.setAccessible(true);

        OAuth2User oauth2User = mock(OAuth2User.class);
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("sub", "123");
        when(oauth2User.getAttributes()).thenReturn(attrs);

        String result = (String) method.invoke(configuration, oauth2User);

        assertNotNull(result);
        assertTrue(result.contains("email="));
        assertTrue(result.contains("name="));
    }

    @Test
    void resolveRedirectTarget_withExternalProviderAndOAuth2User_buildsRegistrationUrl() throws Exception {
        Method method = OAuth2ClientConfiguration.class
                .getDeclaredMethod("resolveRedirectTarget", WebSession.class, Authentication.class, String.class);
        method.setAccessible(true);

        WebSession webSession = mock(WebSession.class);
        Map<String, Object> sessionAttrs = new HashMap<>();
        when(webSession.getAttributes()).thenReturn(sessionAttrs);

        OAuth2User oauth2User = mock(OAuth2User.class);
        Map<String, Object> attrs = Map.of("sub", "123", "email", "user@google.com", "name", "User");
        when(oauth2User.getAttributes()).thenReturn(attrs);

        OAuth2AuthenticationToken authentication = mock(OAuth2AuthenticationToken.class);
        when(authentication.getPrincipal()).thenReturn(oauth2User);

        String result = (String) method.invoke(configuration, webSession, authentication, "google");

        assertNotNull(result);
        assertTrue(result.startsWith("http://localhost:9000/client/registration"));
    }

    @Test
    void resolveRedirectTarget_withSessionRedirectUri_returnsSessionUri() throws Exception {
        Method method = OAuth2ClientConfiguration.class
                .getDeclaredMethod("resolveRedirectTarget", WebSession.class, Authentication.class, String.class);
        method.setAccessible(true);

        WebSession webSession = mock(WebSession.class);
        Map<String, Object> sessionAttrs = new HashMap<>();
        sessionAttrs.put(CustomAuthorizationRequestResolver.SESSION_KEY, "http://custom-redirect.example.com");
        when(webSession.getAttributes()).thenReturn(sessionAttrs);

        Authentication authentication = mock(Authentication.class);

        String result = (String) method.invoke(configuration, webSession, authentication, "track-me-client");

        assertEquals("http://custom-redirect.example.com", result);
    }

    @Test
    void resolveRedirectTarget_noSessionAndInternalProvider_usesAfterLoginUrl() throws Exception {
        Method method = OAuth2ClientConfiguration.class
                .getDeclaredMethod("resolveRedirectTarget", WebSession.class, Authentication.class, String.class);
        method.setAccessible(true);

        WebSession webSession = mock(WebSession.class);
        Map<String, Object> sessionAttrs = new HashMap<>();
        when(webSession.getAttributes()).thenReturn(sessionAttrs);

        Authentication authentication = mock(Authentication.class);

        String result = (String) method.invoke(configuration, webSession, authentication, "track-me-client");

        assertEquals("http://localhost:3000/after-login", result);
    }

    @Test
    void resolveRedirectTarget_externalProviderButNonOAuth2Principal_usesAfterLoginUrl() throws Exception {
        Method method = OAuth2ClientConfiguration.class
                .getDeclaredMethod("resolveRedirectTarget", WebSession.class, Authentication.class, String.class);
        method.setAccessible(true);

        WebSession webSession = mock(WebSession.class);
        Map<String, Object> sessionAttrs = new HashMap<>();
        when(webSession.getAttributes()).thenReturn(sessionAttrs);

        // "google" registration but principal is NOT OAuth2User
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn("some-non-oauth2-principal");

        String result = (String) method.invoke(configuration, webSession, authentication, "google");

        assertEquals("http://localhost:3000/after-login", result);
    }

    @Test
    void buildOAuthRegistrationUrl_withYandexNoEmailAttributes_usesEmptyStrings() throws Exception {
        Method method = OAuth2ClientConfiguration.class
                .getDeclaredMethod("buildOAuthRegistrationUrl", OAuth2User.class);
        method.setAccessible(true);

        OAuth2User oauth2User = mock(OAuth2User.class);
        Map<String, Object> attrs = new HashMap<>();
        // No "sub", no "default_email", no "email", no "real_name", no "display_name"
        when(oauth2User.getAttributes()).thenReturn(attrs);

        String result = (String) method.invoke(configuration, oauth2User);

        assertNotNull(result);
        assertTrue(result.startsWith("http://localhost:9000/client/registration"));
    }
}
