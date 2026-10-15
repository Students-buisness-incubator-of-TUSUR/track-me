package net.trackme.sso.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.Instant;
import net.trackme.sso.services.yandex.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ExtendedModelMap;

class YandexRegistrationControllerTest {
    private final YandexAccountService accounts = mock(YandexAccountService.class);
    private final YandexLoginHandler login = mock(YandexLoginHandler.class);
    private final YandexRegistrationController controller = new YandexRegistrationController(accounts, login);
    private final YandexProfile profile = new YandexProfile("123", "test@yandex.ru", "Test", Instant.now().plusSeconds(600));

    @org.junit.jupiter.api.BeforeEach
    void configureFrontend() { org.springframework.test.util.ReflectionTestUtils.setField(controller, "frontendUri", "http://localhost"); }

    private MockHttpServletRequest pending() {
        var request = new MockHttpServletRequest();
        request.getSession().setAttribute(YandexLoginHandler.PENDING, profile);
        request.getSession().setAttribute("trackme.yandex.linkAttempts", 0);
        return request;
    }

    @Test
    void completionRedirectsToConfiguredFrontend() {
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "frontendUri", "https://trackme.example");
        assertEquals("redirect:https://trackme.example/yandex-registration", controller.form(pending(), new ExtendedModelMap()));
    }

    private org.springframework.security.web.csrf.CsrfToken csrf() {
        return new org.springframework.security.web.csrf.DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-csrf");
    }

    @Test
    void profileOnlyExposesConfirmedEmailAndCsrf() {
        var result = controller.profile(pending(), csrf());
        assertEquals("test@yandex.ru", result.get("email"));
        assertEquals("test-csrf", result.get("csrfToken"));
        assertFalse(result.containsKey("id"));
    }

    @Test
    void apiRejectsMissingProfileAndIncompleteConsents() {
        var body = new YandexRegistrationController.Registration("newuser", "TRACKER", true, true, true);
        assertEquals(400, controller.registration(new MockHttpServletRequest(), body).getStatusCode().value());
        var invalid = new YandexRegistrationController.Registration("newuser", "TRACKER", false, true, true);
        assertEquals(400, controller.registration(pending(), invalid).getStatusCode().value());
        verifyNoInteractions(accounts);
    }

    @Test
    void apiCreatesInactiveAccountAndCannotBeReplayed() {
        var request = pending();
        var body = new YandexRegistrationController.Registration("@newuser", "ADMIN", true, true, true);
        assertEquals(201, controller.registration(request, body).getStatusCode().value());
        assertNull(request.getSession().getAttribute(YandexLoginHandler.PENDING));
        request.removeAttribute("yandex.registered");
        assertEquals(400, controller.registration(request, body).getStatusCode().value());
        verify(accounts, times(1)).register(profile, "newuser", "ADMIN");
    }

    @Test
    void registrationRequiresConsentAndVerifiedProfile() {
        var request = pending();
        controller.register(request, "newuser", "TRACKER", false, false, false);
        verifyNoInteractions(accounts);
        controller.register(new MockHttpServletRequest(), "newuser", "TRACKER", true, true, true);
        verifyNoInteractions(accounts);
        controller.register(request, "newuser", "TRACKER", true, true, true);
        verify(accounts).register(profile, "newuser", "TRACKER");
        assertNull(request.getSession().getAttribute(YandexLoginHandler.PENDING));
    }

    @Test
    void invalidFieldsAreReturnedBesideTheirInputs() {
        var request = pending();
        controller.register(request, "", "SUPER_ADMIN", false, false, false);
        var model = new ExtendedModelMap();
        controller.form(request, model);
        var fields = (java.util.Map<?, ?>) request.getSession().getAttribute("trackme.yandex.fieldErrors");
        assertEquals(5, fields.size());
        assertTrue(fields.containsKey("username"));
        assertTrue(fields.containsKey("role"));
        assertTrue(fields.containsKey("licenseAccepted"));
        assertTrue(fields.containsKey("policyAccepted"));
        assertTrue(fields.containsKey("consent"));
        verifyNoInteractions(accounts);
        assertSame(profile, request.getSession().getAttribute(YandexLoginHandler.PENDING));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"false,true,true", "true,false,true", "true,true,false"})
    void eachConsentIsRequiredOnServer(boolean license, boolean policy, boolean consent) {
        var request = pending();
        controller.register(request, "newuser", "TRACKER", license, policy, consent);
        verifyNoInteractions(accounts);
    }

    @Test
    void stripsLeadingAtAndAcceptsAdminSelection() {
        var request = pending();
        controller.register(request, " @newuser ", "ADMIN", true, true, true);
        verify(accounts).register(profile, "newuser", "ADMIN");
    }

    @Test
    void linkingStopsAfterFiveAttempts() throws Exception {
        var request = pending();
        when(accounts.link(profile, "existing", "wrong")).thenThrow(new IllegalArgumentException("Invalid credentials"));
        for (int i = 0; i < 6; i++) {
            assertEquals("redirect:/client/yandex/complete", controller.link(request, new MockHttpServletResponse(), "existing", "wrong"));
        }
        verify(accounts, times(5)).link(profile, "existing", "wrong");
        verifyNoInteractions(login);
        assertNull(request.getSession().getAttribute(YandexLoginHandler.PENDING));
    }

    @Test
    void expiredProfileIsNotDisplayedOrAccepted() {
        var request = pending();
        request.getSession().setAttribute(YandexLoginHandler.PENDING, new YandexProfile("123", "test@yandex.ru", "Test", Instant.EPOCH));
        request.setParameter("error", "");
        var model = new ExtendedModelMap();
        controller.form(request, model);
        assertEquals(false, controller.profile(request, csrf()).get("pending"));
        assertNotNull(request.getSession().getAttribute("trackme.yandex.error"));
        controller.register(request, "newuser", "TRACKER", true, true, true);
        verifyNoInteractions(accounts);
    }
}
