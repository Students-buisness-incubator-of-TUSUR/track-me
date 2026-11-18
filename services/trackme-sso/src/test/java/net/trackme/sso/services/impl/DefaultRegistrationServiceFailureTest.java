package net.trackme.sso.services.impl;

import net.trackme.sso.components.RegistrationStore;
import net.trackme.sso.components.RegistrationTokenStore;
import net.trackme.sso.dto.RecoveryPasswordRequestDto;
import net.trackme.sso.dto.RegistrationRequestDto;
import net.trackme.sso.dto.RegistrationToken;
import net.trackme.sso.dto.ResetPasswordRequestDto;
import net.trackme.sso.exception.InformationException;
import net.trackme.sso.services.EmailService;
import net.trackme.sso.services.OrganizationService;
import net.trackme.sso.services.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultRegistrationServiceFailureTest {
    private static final String ERROR_MESSAGE = "$happened.unexpected.error";

    @Mock private UserService users;
    @Mock private EmailService email;
    @Mock private RegistrationStore store;
    @Mock private RegistrationTokenStore tokens;
    @Mock private OrganizationService organizations;
    @InjectMocks private DefaultRegistrationService service;

    @Test
    void registrationStorageFailureDoesNotSendConfirmationEmail() {
        var request = new RegistrationRequestDto("tracker", "Password1!", "+79001234567",
                "Tracker", "tracker@example.com", "TRACKER",
                "00000000-0000-0000-0000-000000000001");
        when(organizations.getAvailableOrganization(any())).thenReturn(null);
        when(tokens.generateToken()).thenReturn(new RegistrationToken("hash", "token"));
        doThrow(new IllegalStateException("Redis unavailable")).when(store).saveToRegistration(request, "hash");

        var error = assertThrows(InformationException.class, () -> service.register(request));

        assertEquals(ERROR_MESSAGE, error.getDescription());
        verifyNoInteractions(email);
        verify(users, never()).saveUser(request);
    }

    @Test
    void recoveryStorageFailureDoesNotSendResetEmail() {
        var request = new RecoveryPasswordRequestDto("tracker@example.com");
        when(users.existsByEmail(request.email())).thenReturn(true);
        when(tokens.generateToken()).thenReturn(new RegistrationToken("hash", "token"));
        doThrow(new IllegalStateException("Redis unavailable")).when(store).saveToRecovery(request, "hash");

        var error = assertThrows(InformationException.class, () -> service.recoveryPassword(request));

        assertEquals(ERROR_MESSAGE, error.getDescription());
        verifyNoInteractions(email);
        verify(users, never()).resetPassword(anyString(), anyString());
    }

    @Test
    void invalidConfirmationTokenCannotCreateUser() {
        var request = new MockHttpServletRequest();
        when(tokens.isTokenValid("invalid-token")).thenReturn(false);

        var error = assertThrows(InformationException.class, () -> service.confirm("invalid-token", request));

        assertEquals(ERROR_MESSAGE, error.getDescription());
        verifyNoInteractions(store, users, email);
    }

    @Test
    void invalidRecoveryTokenCannotChangePassword() {
        var request = new ResetPasswordRequestDto("NewPassword1!");
        when(tokens.isTokenValid("invalid-token")).thenReturn(false);

        var error = assertThrows(InformationException.class, () -> service.resetPassword("invalid-token", request));

        assertEquals(ERROR_MESSAGE, error.getDescription());
        verifyNoInteractions(store, users, email);
    }

    @Test
    void trackerRegistrationRequiresOrganization() {
        var request = registration("TRACKER", null);

        var error = assertThrows(InformationException.class, () -> service.register(request));

        assertEquals("$organization.required", error.getDescription());
        verifyNoInteractions(store, email);
        verify(users, never()).saveUser(any());
    }

    @Test
    void registrationRejectsMalformedOrganizationId() {
        var request = registration("ADMIN", "not-a-uuid");

        var error = assertThrows(InformationException.class, () -> service.register(request));

        assertEquals("$organization.not.available", error.getDescription());
        verifyNoInteractions(store, email);
    }

    @Test
    void registrationRejectsBlockedOrExpiredOrganization() {
        var request = registration("SUPER_ADMIN",
                "00000000-0000-0000-0000-000000000002");
        when(organizations.getAvailableOrganization(any()))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("blocked"));

        var error = assertThrows(InformationException.class, () -> service.register(request));

        assertEquals("$organization.not.available", error.getDescription());
        verifyNoInteractions(store, email);
    }

    @Test
    void overlordCannotRegisterThroughPublicRegistration() {
        var request = registration("OVERLORD", null);

        var error = assertThrows(InformationException.class, () -> service.register(request));

        assertEquals("$role.not.available", error.getDescription());
        verifyNoInteractions(store, email);
        verify(users, never()).saveUser(any());
    }

    @Test
    void unknownRoleCannotRegisterThroughPublicRegistration() {
        var request = registration("UNKNOWN", null);

        var error = assertThrows(InformationException.class, () -> service.register(request));

        assertEquals("$role.not.available", error.getDescription());
        verifyNoInteractions(store, email);
        verify(users, never()).saveUser(any());
    }

    private static RegistrationRequestDto registration(String role, String organizationId) {
        return new RegistrationRequestDto("tracker", "Password1!", "+79001234567",
                "Tracker", "tracker@example.com", role, organizationId);
    }
}
