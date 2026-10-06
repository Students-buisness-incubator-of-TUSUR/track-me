package net.trackme.sso.services.yandex;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;
import net.trackme.sso.dao.entity.RoleEntity;
import net.trackme.sso.dao.entity.UserEntity;
import net.trackme.sso.dao.repository.RoleRepository;
import net.trackme.sso.dao.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

class YandexAccountServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final RoleRepository roles = mock(RoleRepository.class);
    private final PasswordEncoder passwords = mock(PasswordEncoder.class);
    private final YandexAccountService service = new YandexAccountService(users, roles, passwords);
    private YandexProfile profile;

    @BeforeEach
    void setUp() {
        profile = new YandexProfile("12345", "person@yandex.ru", "Имя", Instant.now().plusSeconds(600));
    }

    @Test
    void firstLoginDoesNotTrustMatchingEmail() {
        assertTrue(service.login(profile).isEmpty());
        verify(users).findByYandexId("12345");
        verify(users, never()).findByEmail(any());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"TRACKER", "ADMIN"})
    void newUserRequiresActivationAndGetsNoKnownPassword(String roleCode) {
        var role = new RoleEntity();
        role.setCode(roleCode);
        when(roles.findByCode(roleCode)).thenReturn(Optional.of(role));
        when(passwords.encode(any())).thenReturn("random-password-hash");
        service.register(profile, "newuser", "+79001234567", roleCode);
        var saved = ArgumentCaptor.forClass(UserEntity.class);
        verify(users).saveAndFlush(saved.capture());
        assertFalse(saved.getValue().getActive());
        assertEquals("12345", saved.getValue().getYandexId());
        assertEquals("random-password-hash", saved.getValue().getPasswordHash());
        assertEquals(1, saved.getValue().getRoles().size());
    }

    @Test
    void cannotSelectSuperAdminOrUseExpiredProfile() {
        assertThrows(IllegalArgumentException.class, () -> service.register(profile, "newuser", "+79001234567", "SUPER_ADMIN"));
        var expired = new YandexProfile("12345", "person@yandex.ru", "Name", Instant.EPOCH);
        assertThrows(IllegalArgumentException.class, () -> service.login(expired));
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void existingEmailRequiresExplicitLinking() {
        when(users.existsByEmailIgnoreCase(profile.email())).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.register(profile, "newuser", "+79001234567", "TRACKER"));
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void repeatLoginUsesLocalUsernameAndChecksAccountState() {
        var user = user();
        when(users.findByYandexId(profile.id())).thenReturn(Optional.of(user));
        assertEquals("existing-user", service.login(profile).orElseThrow().getUsername());
        user.setActive(false);
        assertThrows(IllegalArgumentException.class, () -> service.login(profile));
        user.setActive(true);
        user.setAccountNonLocked(false);
        assertThrows(IllegalArgumentException.class, () -> service.login(profile));
    }

    @Test
    void linkingRequiresPasswordAndPreservesRoleAndEmail() {
        var user = user();
        when(users.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
        assertThrows(IllegalArgumentException.class, () -> service.link(profile, user.getUsername(), "wrong"));
        assertNull(user.getYandexId());
        when(passwords.matches("correct", "hash")).thenReturn(true);
        assertEquals(user.getUsername(), service.link(profile, user.getUsername(), "correct").getUsername());
        assertEquals("12345", user.getYandexId());
        assertEquals("old@example.org", user.getEmail());
        assertTrue(user.getRoles().isEmpty());
    }

    private UserEntity user() {
        var user = new UserEntity();
        user.setUsername("existing-user");
        user.setEmail("old@example.org");
        user.setPasswordHash("hash");
        user.setActive(true);
        user.setAccountNonLocked(true);
        return user;
    }
}
