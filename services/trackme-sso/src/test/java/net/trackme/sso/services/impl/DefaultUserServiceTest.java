package net.trackme.sso.services.impl;

import net.trackme.commons.filters.FilterRequest;
import net.trackme.sso.AbstractIntegrationTest;
import net.trackme.sso.dto.RegistrationRequestDto;
import net.trackme.sso.exception.AuthException;
import net.trackme.sso.exception.EmailNotFoundException;
import net.trackme.sso.exception.TeamReassignmentException;
import net.trackme.sso.exception.WrongOldPasswordException;
import net.trackme.sso.services.BackendClient;
import net.trackme.sso.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.List;
import java.util.Map;

import static net.trackme.sso.type.AuthErrorCode.ROLE_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * Интеграционные тесты для сервиса управления пользователями.
 * Проверяет операции создания, обновления, блокировки, удаления пользователей
 * и получения информации о них.
 */
class DefaultUserServiceTest extends AbstractIntegrationTest {

    /** Имя пользователя-трекера для тестов. */
    private static final String TRACKER = "tracker";

    /** Email пользователя-трекера для тестов. */
    private static final String TRACKER_EMAIL = "tracker@tracker.com";

    /** Имя пользователя Ronin для тестов. */
    private static final String RONIN = "ronin";

    /** Имя суперадминистратора для тестов. */
    private static final String SUPERADMIN = "superadmin";

    /** Код роли администратора. */
    private static final String ADMIN_ROLE = "ADMIN";

    /** Размер страницы по умолчанию для тестов пагинации. */
    private static final int DEFAULT_PAGE_SIZE = 10;

    /** Сервис для работы с пользователями. */
    @Autowired
    private UserService userService;

    /** Заглушка для клиента взаимодействия с backend. */
    @MockitoBean
    private BackendClient backendClient;

    /**
     * Подготовка тестовых данных перед каждым тестом.
     * Настраивает заглушку BackendClient и создаёт пользователя Ronin,
     * если он отсутствует в тестовой базе данных.
     */
    @BeforeEach
    void setUp() {
        when(backendClient.getUserTeams(anyString(), anyString()))
                .thenReturn(java.util.Collections.emptyList());
        doNothing().when(backendClient)
                .reassignTeamsToRonin(any(), anyString());

        try {
            userService.findByUsername(RONIN);
        } catch (Exception e) {
            var dto = RegistrationRequestDto.builder()
                    .username(RONIN)
                    .password("RoninPass@123")
                    .phoneNumber("+1234567890")
                    .fullName("Ronin User")
                    .email("ronin@tracker.com")
                    .role("TRACKER")
                    .build();
            userService.saveUser(dto);
        }
    }

    // ==================== resetPassword ====================

    /**
     * Тест сброса пароля для несуществующего email.
     * Ожидается исключение EmailNotFoundException.
     */
    @Test
    void resetPasswordEmailNotFound() {
        var email = "john@john.john";
        var password = "testPassword@123";
        assertThrows(EmailNotFoundException.class,
                () -> userService.resetPassword(email, password));
    }

    /**
     * Тест успешного сброса пароля.
     */
    @Test
    @WithMockUser(username = SUPERADMIN, roles = "SUPER_ADMIN")
    void resetPasswordSuccess() {
        var newPassword = "NewPass@123";
        assertDoesNotThrow(() ->
                userService.resetPassword(TRACKER_EMAIL, newPassword));
        var user = userService.findByEmail(TRACKER_EMAIL);
        assertNotNull(user, "User should exist after password reset");
    }

    // ==================== saveUser ====================

    /**
     * Тест успешного сохранения нового пользователя.
     * Проверяет, что пользователь создан с правильными данными и не активен.
     */
    @Test
    @WithMockUser(username = SUPERADMIN, roles = "SUPER_ADMIN")
    void saveUserSuccess() {
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
        assertFalse(savedUser.getActive(),
                "New user should be inactive");
        assertTrue(savedUser.getRoles().stream()
                .anyMatch(role -> role.getCode().equals(ADMIN_ROLE)),
                "User should have ADMIN role");
    }

    /**
     * Тест сохранения пользователя с несуществующей ролью.
     * Ожидается исключение AuthException с кодом ROLE_NOT_FOUND.
     */
    @Test
    void saveUserRoleNotFoundThrowsException() {
        var dto = RegistrationRequestDto.builder()
                .username("testuser")
                .password("Password@123")
                .phoneNumber("+1234567890")
                .fullName("Test User")
                .email("test@test.com")
                .role("NONEXISTENT_ROLE")
                .build();
        var exception = assertThrows(AuthException.class,
                () -> userService.saveUser(dto));
        assertEquals(ROLE_NOT_FOUND, exception.getErrorCode());
    }

    // ==================== changePassword ====================

    /**
     * Тест успешной смены пароля.
     */
    @Test
    @WithMockUser(username = SUPERADMIN, roles = "SUPER_ADMIN")
    void changePasswordSuccess() {
        var knownPassword = "KnownPass@123";
        userService.resetPassword(TRACKER_EMAIL, knownPassword);
        var newPassword = "NewPass@123";
        assertDoesNotThrow(() ->
                userService.changePassword(
                        TRACKER, newPassword, knownPassword));
    }

    /**
     * Тест смены пароля с неверным старым паролем.
     * Ожидается исключение WrongOldPasswordException.
     */
    @Test
    @WithMockUser(username = TRACKER, roles = "TRACKER")
    void changePasswordWrongOldPasswordThrowsException() {
        var wrongOldPassword = "WrongPassword@123";
        var newPassword = "NewPass@123";
        assertThrows(WrongOldPasswordException.class,
                () -> userService.changePassword(
                        TRACKER, newPassword, wrongOldPassword));
    }

    // ==================== save entity ====================

    /**
     * Тест успешного сохранения сущности пользователя.
     */
    @Test
    @WithMockUser(username = SUPERADMIN, roles = "SUPER_ADMIN")
    void saveUserEntitySuccess() {
        var user = userService.findByUsername(TRACKER);
        var originalFullName = user.getFullName();
        user.setFullName("Updated Name");
        assertDoesNotThrow(() -> userService.save(user));
        var updatedUser = userService.findByUsername(TRACKER);
        assertEquals("Updated Name", updatedUser.getFullName());
        updatedUser.setFullName(originalFullName);
        userService.save(updatedUser);
    }

    /**
     * Тест сохранения null-сущности.
     * Ожидается исключение IllegalArgumentException.
     */
    @Test
    void saveNullEntityThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> userService.save(null));
    }

    // ==================== enableUser ====================

    /**
     * Тест успешного включения пользователя.
     */
    @Test
    @WithMockUser(username = SUPERADMIN, roles = "SUPER_ADMIN")
    void enableUserSuccess() {
        var user = userService.findByUsername(TRACKER);
        if (user.getActive()) {
            userService.disableUser(TRACKER);
        }
        assertDoesNotThrow(() -> userService.enableUser(TRACKER));
        var enabledUser = userService.findByUsername(TRACKER);
        assertTrue(enabledUser.getActive());
    }

    /**
     * Тест включения Ronin без прав SUPER_ADMIN.
     * Ожидается исключение AccessDeniedException.
     */
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void enableUserRoninWithoutSuperAdminThrowsException() {
        assertThrows(AccessDeniedException.class,
                () -> userService.enableUser(RONIN));
    }

    /**
     * Тест успешного включения Ronin с правами SUPER_ADMIN.
     */
    @Test
    @WithMockUser(username = SUPERADMIN, roles = "SUPER_ADMIN")
    void enableUserRoninWithSuperAdminSuccess() {
        var roninUser = userService.findByUsername(RONIN);
        if (roninUser.getActive()) {
            userService.disableUser(RONIN);
        }
        assertDoesNotThrow(() -> userService.enableUser(RONIN));
        assertTrue(userService.findByUsername(RONIN).getActive());
    }

    // ==================== disableUser ====================

    /**
     * Тест успешного отключения пользователя.
     */
    @Test
    @WithMockUser(username = SUPERADMIN, roles = "SUPER_ADMIN")
    void disableUserSuccess() {
        var user = userService.findByUsername(TRACKER);
        if (!user.getActive()) {
            userService.enableUser(TRACKER);
        }
        assertDoesNotThrow(() -> userService.disableUser(TRACKER));
        assertFalse(userService.findByUsername(TRACKER).getActive());
    }

    /**
     * Тест отключения активного пользователя.
     * Проверяет, что аккаунт остаётся разблокированным.
     */
    @Test
    @WithMockUser(username = SUPERADMIN, roles = "SUPER_ADMIN")
    void disableUserWhenActiveOnlyDeactivates() {
        var user = userService.findByUsername(TRACKER);
        if (!user.getActive()) {
            userService.enableUser(TRACKER);
        }
        user = userService.findByUsername(TRACKER);
        user.setAccountNonLocked(true);
        userService.save(user);

        userService.disableUser(TRACKER);

        var result = userService.findByUsername(TRACKER);
        assertFalse(result.getActive());
        assertTrue(result.getAccountNonLocked(),
                "Account should remain unlocked when user was active");
    }

    /**
     * Тест отключения Ronin без прав SUPER_ADMIN.
     * Ожидается исключение AccessDeniedException.
     */
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void disableUserRoninWithoutSuperAdminThrowsException() {
        assertThrows(AccessDeniedException.class,
                () -> userService.disableUser(RONIN));
    }

    /**
     * Тест успешного отключения Ronin с правами SUPER_ADMIN.
     */
    @Test
    @WithMockUser(username = SUPERADMIN, roles = "SUPER_ADMIN")
    void disableUserRoninWithSuperAdminSuccess() {
        var roninUser = userService.findByUsername(RONIN);
        if (!roninUser.getActive()) {
            userService.enableUser(RONIN);
        }
        userService.disableUser(RONIN);
        assertFalse(userService.findByUsername(RONIN).getActive());
    }

    /**
     * Тест отключения уже неактивного пользователя.
     * Проверяет, что аккаунт блокируется.
     */
    @Test
    @WithMockUser(username = SUPERADMIN, roles = "SUPER_ADMIN")
    void disableUserAlreadyDisabledLocksAccount() {
        var user = userService.findByUsername(TRACKER);
        if (user.getActive()) {
            userService.disableUser(TRACKER);
        }
        user = userService.findByUsername(TRACKER);
        assertFalse(user.getActive());
        user.setAccountNonLocked(true);
        userService.save(user);
        assertDoesNotThrow(() -> userService.disableUser(TRACKER));
        assertFalse(userService.findByUsername(TRACKER)
                .getAccountNonLocked());
    }

    /**
     * Тест отключения уже неактивного Ronin.
     * Ожидается исключение TeamReassignmentException.
     */
    @Test
    @WithMockUser(username = SUPERADMIN, roles = "SUPER_ADMIN")
    void disableUserRoninAlreadyDisabledThrowsException() {
        var roninUser = userService.findByUsername(RONIN);
        if (!roninUser.getActive()) {
            userService.enableUser(RONIN);
        }

        userService.disableUser(RONIN);

        assertThrows(TeamReassignmentException.class,
                () -> userService.disableUser(RONIN));

        userService.enableUser(RONIN);
    }

    /**
     * Тест отключения Ronin при наличии у него команд.
     * Ожидается исключение TeamReassignmentException.
     */
    @Test
    @WithMockUser(username = SUPERADMIN, roles = "SUPER_ADMIN")
    void disableRoninWithTeamsThrowsException() {
        when(backendClient.getUserTeams(eq(RONIN), anyString()))
                .thenReturn(List.of(
                        Map.of("id", "team1", "name", "Team 1")));

        var roninUser = userService.findByUsername(RONIN);
        if (!roninUser.getActive()) {
            userService.enableUser(RONIN);
        }

        assertThrows(TeamReassignmentException.class,
                () -> userService.disableUser(RONIN));

        reset(backendClient);
        when(backendClient.getUserTeams(anyString(), anyString()))
                .thenReturn(java.util.Collections.emptyList());
        doNothing().when(backendClient)
                .reassignTeamsToRonin(any(), anyString());
    }

    // ==================== unlockUser ====================

    /**
     * Тест успешной разблокировки пользователя.
     */
    @Test
    @WithMockUser(username = SUPERADMIN, roles = "SUPER_ADMIN")
    void unlockUserSuccess() {
        userService.enableUser(TRACKER);
        userService.disableUser(TRACKER);
        userService.disableUser(TRACKER);
        assertDoesNotThrow(() -> userService.unlockUser(TRACKER));
        var user = userService.findByUsername(TRACKER);
        assertTrue(user.getAccountNonLocked());
        assertFalse(user.getActive());
    }

    /**
     * Тест разблокировки уже разблокированного пользователя.
     */
    @Test
    @WithMockUser(username = SUPERADMIN, roles = "SUPER_ADMIN")
    void unlockUserAlreadyUnlockedDoesNothing() {
        userService.enableUser(TRACKER);
        assertDoesNotThrow(() -> userService.unlockUser(TRACKER));
        assertTrue(userService.findByUsername(TRACKER)
                .getAccountNonLocked());
    }

    /**
     * Тест разблокировки Ronin без прав SUPER_ADMIN.
     * Ожидается исключение AccessDeniedException.
     */
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void unlockUserRoninWithoutSuperAdminThrowsException() {
        assertThrows(AccessDeniedException.class,
                () -> userService.unlockUser(RONIN));
    }

    /**
     * Тест успешной разблокировки Ronin с правами SUPER_ADMIN.
     */
    @Test
    @WithMockUser(username = SUPERADMIN, roles = "SUPER_ADMIN")
    void unlockUserRoninWithSuperAdminSuccess() {
        userService.unlockUser(RONIN);
        assertTrue(userService.findByUsername(RONIN)
                .getAccountNonLocked());
    }

    // ==================== deleteUser ====================

    /**
     * Тест удаления Ronin без аутентификации.
     * Ожидается исключение AccessDeniedException.
     */
    @Test
    void deleteUserRoninNoAuthThrowsException() {
        assertThrows(AccessDeniedException.class,
                () -> userService.deleteUser(RONIN));
    }

    /**
     * Тест удаления Ronin без прав SUPER_ADMIN.
     * Ожидается исключение AccessDeniedException.
     */
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void deleteUserRoninWithoutSuperAdminThrowsException() {
        assertThrows(AccessDeniedException.class,
                () -> userService.deleteUser(RONIN));
    }

    /**
     * Тест успешного удаления пользователя.
     */
    @Test
    @WithMockUser(username = SUPERADMIN, roles = "SUPER_ADMIN")
    void deleteUserSuccess() {
        var dto = RegistrationRequestDto.builder()
                .username("todelete")
                .password("Password@123")
                .phoneNumber("+1234567890")
                .fullName("To Delete")
                .email("todelete@test.com")
                .role(ADMIN_ROLE)
                .build();
        userService.saveUser(dto);

        assertDoesNotThrow(() ->
                userService.findByUsername("todelete"));
        assertDoesNotThrow(() ->
                userService.deleteUser("todelete"));
        assertThrows(Exception.class, () ->
                userService.findByUsername("todelete"));
    }

    /**
     * Тест удаления пользователя, у которого есть команды.
     */
    @Test
    @WithMockUser(username = SUPERADMIN, roles = "SUPER_ADMIN")
    void deleteUserWithTeamsSuccess() {
        when(backendClient.getUserTeams(anyString(), anyString()))
                .thenReturn(List.of(
                        Map.of("id", "team1", "name", "Team 1")));

        var dto = RegistrationRequestDto.builder()
                .username("todelete7")
                .password("Password@123")
                .phoneNumber("+1234567890")
                .fullName("To Delete 7")
                .email("todelete7@test.com")
                .role(ADMIN_ROLE)
                .build();
        userService.saveUser(dto);

        assertDoesNotThrow(() ->
                userService.deleteUser("todelete7"));
        assertThrows(Exception.class, () ->
                userService.findByUsername("todelete7"));

        reset(backendClient);
        when(backendClient.getUserTeams(anyString(), anyString()))
                .thenReturn(java.util.Collections.emptyList());
        doNothing().when(backendClient)
                .reassignTeamsToRonin(any(), anyString());
    }

    /**
     * Тест удаления пользователя при ошибке переназначения команд.
     * Удаление должно пройти успешно, несмотря на ошибку backend.
     */
    @Test
    @WithMockUser(username = SUPERADMIN, roles = "SUPER_ADMIN")
    void deleteUserReassignTeamsFailsSuccess() {
        doThrow(new RuntimeException("Backend error"))
                .when(backendClient)
                .reassignTeamsToRonin(any(), anyString());

        var dto = RegistrationRequestDto.builder()
                .username("todelete4")
                .password("Password@123")
                .phoneNumber("+1234567890")
                .fullName("To Delete 4")
                .email("todelete4@test.com")
                .role(ADMIN_ROLE)
                .build();
        userService.saveUser(dto);

        assertDoesNotThrow(() ->
                userService.deleteUser("todelete4"));
        assertThrows(Exception.class, () ->
                userService.findByUsername("todelete4"));

        reset(backendClient);
        when(backendClient.getUserTeams(anyString(), anyString()))
                .thenReturn(java.util.Collections.emptyList());
        doNothing().when(backendClient)
                .reassignTeamsToRonin(any(), anyString());
    }

    /**
     * Тест удаления пользователя при выбросе
     * TeamReassignmentException из backend.
     * Ожидается проброс исключения.
     */
    @Test
    @WithMockUser(username = SUPERADMIN, roles = "SUPER_ADMIN")
    void deleteUserReassignTeamsThrowsException() {
        doThrow(new TeamReassignmentException(
                "Test reassign exception"))
                .when(backendClient)
                .reassignTeamsToRonin(any(), anyString());

        var dto = RegistrationRequestDto.builder()
                .username("todelete8")
                .password("Password@123")
                .phoneNumber("+1234567890")
                .fullName("To Delete 8")
                .email("todelete8@test.com")
                .role(ADMIN_ROLE)
                .build();
        userService.saveUser(dto);

        assertThrows(TeamReassignmentException.class,
                () -> userService.deleteUser("todelete8"));

        reset(backendClient);
        when(backendClient.getUserTeams(anyString(), anyString()))
                .thenReturn(java.util.Collections.emptyList());
        doNothing().when(backendClient)
                .reassignTeamsToRonin(any(), anyString());
    }

    /**
     * Тест удаления пользователя при ошибке получения списка команд.
     * Удаление должно пройти успешно.
     */
    @Test
    @WithMockUser(username = SUPERADMIN, roles = "SUPER_ADMIN")
    void deleteUserLogUserTeamsFailsSuccess() {
        when(backendClient.getUserTeams(anyString(), anyString()))
                .thenThrow(new RuntimeException("Backend error"));

        var dto = RegistrationRequestDto.builder()
                .username("todelete9")
                .password("Password@123")
                .phoneNumber("+1234567890")
                .fullName("To Delete 9")
                .email("todelete9@test.com")
                .role(ADMIN_ROLE)
                .build();
        userService.saveUser(dto);

        assertDoesNotThrow(() ->
                userService.deleteUser("todelete9"));
        assertThrows(Exception.class, () ->
                userService.findByUsername("todelete9"));

        reset(backendClient);
        when(backendClient.getUserTeams(anyString(), anyString()))
                .thenReturn(java.util.Collections.emptyList());
        doNothing().when(backendClient)
                .reassignTeamsToRonin(any(), anyString());
    }

    // ==================== findByUsername / findByEmail ====================

    /**
     * Тест поиска несуществующего пользователя по имени.
     * Ожидается исключение.
     */
    @Test
    void findByUsernameNotFoundThrowsException() {
        assertThrows(Exception.class,
                () -> userService.findByUsername(
                        "nonexistent_user_12345"));
    }

    /**
     * Тест поиска несуществующего пользователя по email.
     * Ожидается исключение EmailNotFoundException.
     */
    @Test
    void findByEmailNotFoundThrowsException() {
        assertThrows(EmailNotFoundException.class,
                () -> userService.findByEmail(
                        "nonexistent@email.com"));
    }

    /**
     * Тест успешного поиска существующего пользователя по имени.
     */
    @Test
    void findByUsernameExistingUserReturnsUser() {
        var user = userService.findByUsername(TRACKER);
        assertNotNull(user);
        assertEquals(TRACKER, user.getUsername());
    }

    // ==================== getUserTeams ====================

    /**
     * Тест получения списка команд пользователя.
     */
    @Test
    @WithMockUser(username = SUPERADMIN, roles = "SUPER_ADMIN")
    void getUserTeamsReturnsList() {
        var teams = userService.getUserTeams(TRACKER);
        assertNotNull(teams, "Teams list should not be null");
    }

    /**
     * Тест проброса TeamReassignmentException из getUserTeams.
     */
    @Test
    @WithMockUser(username = SUPERADMIN, roles = "SUPER_ADMIN")
    void getUserTeamsPropagatesException() {
        when(backendClient.getUserTeams(anyString(), anyString()))
                .thenThrow(new TeamReassignmentException(
                        "Test exception"));

        assertThrows(TeamReassignmentException.class,
                () -> userService.getUserTeams(TRACKER));

        reset(backendClient);
        when(backendClient.getUserTeams(anyString(), anyString()))
                .thenReturn(java.util.Collections.emptyList());
        doNothing().when(backendClient)
                .reassignTeamsToRonin(any(), anyString());
    }

    /**
     * Тест получения команд при ошибке backend.
     * Ожидается пустой список.
     */
    @Test
    @WithMockUser(username = SUPERADMIN, roles = "SUPER_ADMIN")
    void getUserTeamsBackendExceptionReturnsEmptyList() {
        when(backendClient.getUserTeams(anyString(), anyString()))
                .thenThrow(new RuntimeException("Backend error"));

        var teams = userService.getUserTeams(TRACKER);
        assertNotNull(teams);
        assertTrue(teams.isEmpty(),
                "Should return empty list when backend throws exception");

        reset(backendClient);
        when(backendClient.getUserTeams(anyString(), anyString()))
                .thenReturn(java.util.Collections.emptyList());
        doNothing().when(backendClient)
                .reassignTeamsToRonin(any(), anyString());
    }

    /**
     * Тест получения команд при отсутствии контекста запроса.
     * Ожидается пустой список.
     */
    @Test
    void getUserTeamsNoRequestContextReturnsEmptyList() {
        var oldAttributes =
                RequestContextHolder.getRequestAttributes();
        try {
            RequestContextHolder.resetRequestAttributes();
            var teams = userService.getUserTeams(TRACKER);
            assertNotNull(teams);
            assertTrue(teams.isEmpty());
        } finally {
            if (oldAttributes != null) {
                RequestContextHolder.setRequestAttributes(
                        oldAttributes);
            }
        }
    }

    // ==================== exists ====================

    /**
     * Тест проверки существования пользователя по email и имени.
     * Ожидается true для существующего пользователя.
     */
    @Test
    void existsByEmailOrUsernameExistingUserReturnsTrue() {
        assertTrue(userService.existsByEmailOrUsername(
                TRACKER_EMAIL, TRACKER));
    }

    /**
     * Тест проверки существования несуществующего пользователя.
     * Ожидается false.
     */
    @Test
    void existsByEmailOrUsernameNonExistingReturnsFalse() {
        assertFalse(userService.existsByEmailOrUsername(
                "no@no.com", "nouser"));
    }

    /**
     * Тест проверки существования пользователя по email.
     * Ожидается true для существующего email.
     */
    @Test
    void existsByEmailExistingUserReturnsTrue() {
        assertTrue(userService.existsByEmail(TRACKER_EMAIL));
    }

    /**
     * Тест проверки существования несуществующего email.
     * Ожидается false.
     */
    @Test
    void existsByEmailNonExistingReturnsFalse() {
        assertFalse(userService.existsByEmail("no@no.com"));
    }

    // ==================== getUserInfo ====================

    /**
     * Тест получения информации о пользователе.
     * Проверяет корректность возвращаемых данных.
     */
    @Test
    void getUserInfoReturnsCorrectData() {
        var dto = userService.getUserInfo(TRACKER);
        assertNotNull(dto);
        assertEquals(TRACKER, dto.username());
        assertEquals(TRACKER_EMAIL, dto.email());
    }

    /**
     * Тест получения информации о пользователе с ролями.
     * Проверяет наличие роли SUPER_ADMIN.
     */
    @Test
    void getUserInfoWithRolesReturnsCorrectRoles() {
        var dto = userService.getUserInfo(SUPERADMIN);
        assertNotNull(dto);
        assertNotNull(dto.roles());
        assertFalse(dto.roles().isEmpty());
        assertTrue(dto.roles().stream()
                .anyMatch(r -> r.equals("SUPER_ADMIN")));
    }

    // ==================== getTrackers / getAdmins ====================

    /**
     * Тест получения страницы трекеров.
     */
    @Test
    void getTrackersReturnsPage() {
        var page = userService.getTrackers(
                new FilterRequest(List.of()),
                Pageable.ofSize(DEFAULT_PAGE_SIZE));
        assertNotNull(page);
        assertTrue(page.getTotalElements() >= 0);
    }

    /**
     * Тест получения страницы администраторов.
     */
    @Test
    void getAdminsReturnsPage() {
        var page = userService.getAdmins(
                new FilterRequest(List.of()),
                Pageable.ofSize(DEFAULT_PAGE_SIZE));
        assertNotNull(page);
        assertTrue(page.getTotalElements() >= 0);
    }

    // ==================== TeamReassignmentException ====================

    /**
     * Тест создания TeamReassignmentException с причиной.
     */
    @Test
    void teamReassignmentExceptionWithCause() {
        var cause = new RuntimeException("Root cause");
        var exception = new TeamReassignmentException(
                "Test message", cause);
        assertEquals("Test message", exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    /**
     * Тест создания TeamReassignmentException с сообщением.
     */
    @Test
    void teamReassignmentExceptionWithMessage() {
        var exception = new TeamReassignmentException(
                "Test message");
        assertEquals("Test message", exception.getMessage());
        assertNull(exception.getCause());
    }
}
