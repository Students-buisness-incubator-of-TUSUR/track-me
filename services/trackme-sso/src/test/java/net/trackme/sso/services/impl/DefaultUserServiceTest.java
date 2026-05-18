package net.trackme.sso.services.impl;

import net.trackme.commons.filters.FilterRequest;
import net.trackme.sso.AbstractIntegrationTest;
import net.trackme.sso.dto.RegistrationRequestDto;
import net.trackme.sso.exception.AuthException;
import net.trackme.sso.exception.EmailNotFoundException;
import net.trackme.sso.exception.WrongOldPasswordException;
import net.trackme.sso.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;

import static net.trackme.sso.type.AuthErrorCode.ROLE_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.*;

class DefaultUserServiceTest extends AbstractIntegrationTest {

    private static final String TRACKER = "tracker";
    private static final String TRACKER_EMAIL = "tracker@tracker.com";
    private static final String RONIN = "ronin";
    private static final String SUPERADMIN = "superadmin";
    private static final String ADMIN_ROLE = "ADMIN";

    @Autowired
    private UserService userService;

    @Test
    void resetPassword_emailNotFound() {
        var email = "john@john.john";
        var password = "testPassword@123";
        assertThrows(EmailNotFoundException.class, () -> userService.resetPassword(email, password));
    }

    @Test
    @WithMockUser(username = SUPERADMIN, roles = "SUPER_ADMIN")
    void resetPassword_success() {
        var newPassword = "NewPass@123";
        assertDoesNotThrow(() -> userService.resetPassword(TRACKER_EMAIL, newPassword));
        var user = userService.findByEmail(TRACKER_EMAIL);
        assertNotNull(user, "User should exist after password reset");
    }

    @Test
    @WithMockUser(username = SUPERADMIN, roles = "SUPER_ADMIN")
    void saveUser_success() {
        var dto = RegistrationRequestDto.builder()
            .username("newuser")
            .password("Password@123")
            .phoneNumber("+1234567890")
            .fullName("New User")
            .email("newuser@test.com")
            .role(ADMIN_ROLE)
            .build();
        var savedUser = userService.saveUser(dto);
        assertNotNull(savedUser, "Saved user should not be null");
        assertEquals("newuser@test.com", savedUser.getEmail());
        assertEquals("newuser", savedUser.getUsername());
        assertFalse(savedUser.getActive(), "New user should be inactive");
        assertTrue(savedUser.getRoles().stream()
            .anyMatch(role -> role.getCode().equals(ADMIN_ROLE)), "User should have ADMIN role");
    }

    @Test
    void saveUser_roleNotFound_throwsException() {
        var dto = RegistrationRequestDto.builder()
            .username("testuser")
            .password("Password@123")
            .phoneNumber("+1234567890")
            .fullName("Test User")
            .email("test@test.com")
            .role("NONEXISTENT_ROLE")
            .build();
        var exception = assertThrows(AuthException.class, () -> userService.saveUser(dto));
        assertEquals(ROLE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @WithMockUser(username = SUPERADMIN, roles = "SUPER_ADMIN")
    void changePassword_success() {
        var knownPassword = "KnownPass@123";
        userService.resetPassword(TRACKER_EMAIL, knownPassword);
        var newPassword = "NewPass@123";
        assertDoesNotThrow(() -> userService.changePassword(TRACKER, newPassword, knownPassword));
    }

    @Test
    @WithMockUser(username = TRACKER, roles = "TRACKER")
    void changePassword_wrongOldPassword_throwsException() {
        var wrongOldPassword = "WrongPassword@123";
        var newPassword = "NewPass@123";
        assertThrows(WrongOldPasswordException.class,
            () -> userService.changePassword(TRACKER, newPassword, wrongOldPassword));
    }

    @Test
    @WithMockUser(username = SUPERADMIN, roles = "SUPER_ADMIN")
    void save_userEntity_success() {
        var user = userService.findByUsername(TRACKER);
        var originalFullName = user.getFullName();
        user.setFullName("Updated Name");
        assertDoesNotThrow(() -> userService.save(user));
        var updatedUser = userService.findByUsername(TRACKER);
        assertEquals("Updated Name", updatedUser.getFullName());
        updatedUser.setFullName(originalFullName);
        userService.save(updatedUser);
    }

    @Test
    void save_nullEntity_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> userService.save(null));
    }

    @Test
    @WithMockUser(username = SUPERADMIN, roles = "SUPER_ADMIN")
    void enableUser_success() {
        var user = userService.findByUsername(TRACKER);
        if (user.getActive()) {
            userService.disableUser(TRACKER);
        }
        assertDoesNotThrow(() -> userService.enableUser(TRACKER));
        var enabledUser = userService.findByUsername(TRACKER);
        assertTrue(enabledUser.getActive());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void enableUser_roninWithoutSuperAdmin_throwsException() {
        assertThrows(AccessDeniedException.class, () -> userService.enableUser(RONIN));
    }

    @Test
    @WithMockUser(username = SUPERADMIN, roles = "SUPER_ADMIN")
    void enableUser_roninWithSuperAdmin_success() {
        var roninUser = userService.findByUsername(RONIN);
        if (roninUser.getActive()) {
            userService.disableUser(RONIN);
        }
        assertDoesNotThrow(() -> userService.enableUser(RONIN));
        assertTrue(userService.findByUsername(RONIN).getActive());
    }

    @Test
    @WithMockUser(username = SUPERADMIN, roles = "SUPER_ADMIN")
    void disableUser_success() {
        var user = userService.findByUsername(TRACKER);
        if (!user.getActive()) {
            userService.enableUser(TRACKER);
        }
        assertDoesNotThrow(() -> userService.disableUser(TRACKER));
        assertFalse(userService.findByUsername(TRACKER).getActive());
    }

    @Test
    @WithMockUser(username = SUPERADMIN, roles = "SUPER_ADMIN")
    void disableUser_whenActive_onlyDeactivates() {
        var user = userService.findByUsername(TRACKER);
        if (!user.getActive()) {
            userService.enableUser(TRACKER);
        }
        // Получаем свежую версию после enable
        user = userService.findByUsername(TRACKER);
        user.setAccountNonLocked(true);
        userService.save(user);
        
        userService.disableUser(TRACKER);
        
        var result = userService.findByUsername(TRACKER);
        assertFalse(result.getActive());
        assertTrue(result.getAccountNonLocked(), "Account should remain unlocked when user was active");
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void disableUser_roninWithoutSuperAdmin_throwsException() {
        assertThrows(AccessDeniedException.class, () -> userService.disableUser(RONIN));
    }

    @Test
    @WithMockUser(username = SUPERADMIN, roles = "SUPER_ADMIN")
    void disableUser_roninWithSuperAdmin_success() {
        var roninUser = userService.findByUsername(RONIN);
        if (!roninUser.getActive()) {
            userService.enableUser(RONIN);
        }
        userService.disableUser(RONIN);
        assertFalse(userService.findByUsername(RONIN).getActive());
    }

    @Test
    @WithMockUser(username = SUPERADMIN, roles = "SUPER_ADMIN")
    void disableUser_alreadyDisabled_locksAccount() {
        var user = userService.findByUsername(TRACKER);
        if (user.getActive()) {
            userService.disableUser(TRACKER);
        }
        user = userService.findByUsername(TRACKER);
        assertFalse(user.getActive());
        user.setAccountNonLocked(true);
        userService.save(user);
        assertDoesNotThrow(() -> userService.disableUser(TRACKER));
        assertFalse(userService.findByUsername(TRACKER).getAccountNonLocked());
    }

    @Test
    @WithMockUser(username = SUPERADMIN, roles = "SUPER_ADMIN")
    void unlockUser_success() {
        userService.enableUser(TRACKER);
        userService.disableUser(TRACKER);
        userService.disableUser(TRACKER);
        assertDoesNotThrow(() -> userService.unlockUser(TRACKER));
        var user = userService.findByUsername(TRACKER);
        assertTrue(user.getAccountNonLocked());
        assertFalse(user.getActive());
    }

    @Test
    @WithMockUser(username = SUPERADMIN, roles = "SUPER_ADMIN")
    void unlockUser_alreadyUnlocked_doesNothing() {
        userService.enableUser(TRACKER);
        assertDoesNotThrow(() -> userService.unlockUser(TRACKER));
        assertTrue(userService.findByUsername(TRACKER).getAccountNonLocked());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void unlockUser_roninWithoutSuperAdmin_throwsException() {
        assertThrows(AccessDeniedException.class, () -> userService.unlockUser(RONIN));
    }

    @Test
    @WithMockUser(username = SUPERADMIN, roles = "SUPER_ADMIN")
    void unlockUser_roninWithSuperAdmin_success() {
        userService.unlockUser(RONIN);
        assertTrue(userService.findByUsername(RONIN).getAccountNonLocked());
    }

    @Test
    void deleteUser_ronin_noAuth_throwsException() {
        assertThrows(AccessDeniedException.class, () -> userService.deleteUser(RONIN));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void deleteUser_roninWithoutSuperAdmin_throwsException() {
        assertThrows(AccessDeniedException.class, () -> userService.deleteUser(RONIN));
    }

    @Test
    @WithMockUser(username = SUPERADMIN, roles = "SUPER_ADMIN")
    void deleteUser_success() {
        // Создаём пользователя
        var dto = RegistrationRequestDto.builder()
            .username("todelete")
            .password("Password@123")
            .phoneNumber("+1234567890")
            .fullName("To Delete")
            .email("todelete@test.com")
            .role(ADMIN_ROLE)
            .build();
        userService.saveUser(dto);
        
        // Проверяем, что пользователь создан
        assertDoesNotThrow(() -> userService.findByUsername("todelete"));
        
        // Удаляем пользователя - не должно быть исключений
        assertDoesNotThrow(() -> userService.deleteUser("todelete"));
        
        // Проверяем, что пользователь действительно удалён
        assertThrows(Exception.class, () -> userService.findByUsername("todelete"));
    }

    @Test
    void findByUsername_notFound_throwsException() {
        assertThrows(Exception.class, () -> userService.findByUsername("nonexistent_user_12345"));
    }

    @Test
    void findByEmail_notFound_throwsException() {
        assertThrows(EmailNotFoundException.class, 
            () -> userService.findByEmail("nonexistent@email.com"));
    }

    @Test
    @WithMockUser(username = SUPERADMIN, roles = "SUPER_ADMIN")
    void getUserTeams_returnsList() {
        var teams = userService.getUserTeams(TRACKER);
        assertNotNull(teams, "Teams list should not be null");
    }

    @Test
    void existsByEmailOrUsername_existingUser_returnsTrue() {
        assertTrue(userService.existsByEmailOrUsername(TRACKER_EMAIL, TRACKER));
    }

    @Test
    void existsByEmailOrUsername_nonExisting_returnsFalse() {
        assertFalse(userService.existsByEmailOrUsername("no@no.com", "nouser"));
    }

    @Test
    void existsByEmail_existingUser_returnsTrue() {
        assertTrue(userService.existsByEmail(TRACKER_EMAIL));
    }

    @Test
    void existsByEmail_nonExisting_returnsFalse() {
        assertFalse(userService.existsByEmail("no@no.com"));
    }

    @Test
    void findByUsername_existingUser_returnsUser() {
        var user = userService.findByUsername(TRACKER);
        assertNotNull(user);
        assertEquals(TRACKER, user.getUsername());
    }

    @Test
    void getUserInfo_returnsCorrectData() {
        var dto = userService.getUserInfo(TRACKER);
        assertNotNull(dto);
        assertEquals(TRACKER, dto.username());
        assertEquals(TRACKER_EMAIL, dto.email());
    }

    @Test
    void getUserInfo_withRoles_returnsCorrectRoles() {
        var dto = userService.getUserInfo(SUPERADMIN);
        assertNotNull(dto);
        assertNotNull(dto.roles());
        assertFalse(dto.roles().isEmpty());
        assertTrue(dto.roles().stream().anyMatch(r -> r.equals("SUPER_ADMIN")));
    }

    @Test
    void getTrackers_returnsPage() {
        var page = userService.getTrackers(
                new FilterRequest(List.of()), Pageable.ofSize(10));
        assertNotNull(page);
        assertTrue(page.getTotalElements() >= 0);
    }

    @Test
    void getAdmins_returnsPage() {
        var page = userService.getAdmins(
                new FilterRequest(List.of()), Pageable.ofSize(10));
        assertNotNull(page);
        assertTrue(page.getTotalElements() >= 0);
    }
}
