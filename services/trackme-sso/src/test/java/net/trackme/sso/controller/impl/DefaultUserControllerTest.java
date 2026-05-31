package net.trackme.sso.controller.impl;

import net.trackme.sso.AbstractIntegrationTest;
import net.trackme.sso.dao.entity.UserEntity;
import net.trackme.sso.dto.RegistrationRequestDto;
import net.trackme.sso.services.BackendClient;
import net.trackme.sso.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционные тесты для контроллера управления пользователями.
 * Проверяет операции включения, отключения, удаления пользователей
 * и получения информации о них.
 */
class DefaultUserControllerTest extends AbstractIntegrationTest {

    /** Имя пользователя-трекера для тестов. */
    private static final String TRACKER = "tracker";

    /** Имя пользователя Ronin для тестов. */
    private static final String RONIN = "ronin";

    /** HTTP-статус 409 Conflict. */
    private static final int HTTP_CONFLICT = 409;

    /** MockMvc для выполнения HTTP-запросов в тестах. */
    @Autowired
    private MockMvc mockMvc;

    /** Сервис загрузки данных пользователя. */
    @Autowired
    private UserDetailsService userDetailsService;

    /** Сервис для работы с пользователями. */
    @Autowired
    private UserService userService;

    /** Заглушка для BackendClient. */
    @MockitoBean
    private BackendClient backendClient;

    /**
     * Подготовка тестовых данных перед каждым тестом.
     * Настраивает заглушку BackendClient и создаёт/сбрасывает
     * тестовых пользователей.
     */
    @BeforeEach
    void setUp() {
        reset(backendClient);
        when(backendClient.getUserTeams(anyString(), anyString()))
                .thenReturn(java.util.Collections.emptyList());
        doNothing().when(backendClient)
                .reassignTeamsToRonin(any(), anyString());

        try {
            UserEntity tracker = userService.findByUsername(TRACKER);
            if (tracker != null) {
                userService.disableUser(TRACKER);
                if (!tracker.getAccountNonLocked()) {
                    userService.unlockUser(TRACKER);
                }
            }
        } catch (UsernameNotFoundException e) {
            // Пользователь tracker не найден в тестовой БД — OK
        }

        try {
            userService.findByUsername(RONIN);
        } catch (UsernameNotFoundException e) {
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

    /**
     * Тест успешного включения пользователя.
     *
     * @throws Exception если произошла ошибка при выполнении запроса
     */
    @Test
    @WithMockUser(username = "superadmin", roles = "SUPER_ADMIN")
    void enableUserSuccess() throws Exception {
        mockMvc.perform(post("/api/v1/users/enable")
                .param("username", TRACKER)
                .with(csrf()))
                .andExpect(status().isOk());

        assertThat(userDetailsService.loadUserByUsername(TRACKER)
                .isEnabled()).isTrue();

        mockMvc.perform(post("/api/v1/users/disable")
                .param("username", TRACKER)
                .with(csrf()))
                .andExpect(status().isOk());
    }

    /**
     * Тест успешного отключения пользователя.
     *
     * @throws Exception если произошла ошибка при выполнении запроса
     */
    @Test
    @WithMockUser(username = "superadmin", roles = "SUPER_ADMIN")
    void disableUserSuccess() throws Exception {
        mockMvc.perform(post("/api/v1/users/enable")
                .param("username", TRACKER)
                .with(csrf()))
                .andExpect(status().isOk());

        assertThat(userDetailsService.loadUserByUsername(TRACKER)
                .isEnabled()).isTrue();

        mockMvc.perform(post("/api/v1/users/disable")
                .param("username", TRACKER)
                .with(csrf()))
                .andExpect(status().isOk());

        assertThat(userDetailsService.loadUserByUsername(TRACKER)
                .isEnabled()).isFalse();

        mockMvc.perform(post("/api/v1/users/disable")
                .param("username", TRACKER)
                .with(csrf()))
                .andExpect(status().isOk());

        assertThat(userService.findByUsername(TRACKER)
                .getAccountNonLocked()).isFalse();

        mockMvc.perform(post("/api/v1/users/enable")
                .param("username", TRACKER)
                .with(csrf()))
                .andExpect(status().isOk());
    }

    /**
     * Тест включения несуществующего пользователя.
     *
     * @throws Exception если произошла ошибка при выполнении запроса
     */
    @Test
    @WithMockUser(username = "superadmin", roles = "SUPER_ADMIN")
    void enableUserNotFound() throws Exception {
        mockMvc.perform(post("/api/v1/users/enable")
                .param("username", "notfound")
                .with(csrf()))
                .andExpect(status().isNotFound());
    }

    /**
     * Тест включения пользователя без прав SUPER_ADMIN.
     *
     * @throws Exception если произошла ошибка при выполнении запроса
     */
    @Test
    @WithMockUser(username = TRACKER, roles = "TRACKER")
    void enableUserNotSuperAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/users/enable")
                .param("username", TRACKER)
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    /**
     * Тест отключения пользователя без прав SUPER_ADMIN.
     *
     * @throws Exception если произошла ошибка при выполнении запроса
     */
    @Test
    @WithMockUser(username = TRACKER, roles = "TRACKER")
    void disableUserNotSuperAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/users/disable")
                .param("username", TRACKER)
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    /**
     * Тест успешного получения информации о пользователе.
     *
     * @throws Exception если произошла ошибка при выполнении запроса
     */
    @Test
    @WithMockUser(username = "superadmin", roles = "SUPER_ADMIN")
    void getUserInfoSuccess() throws Exception {
        mockMvc.perform(get("/api/v1/users/{username}/info", TRACKER))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/json"))
                .andExpect(jsonPath("$.username").value(TRACKER));
    }

    /**
     * Тест получения информации без прав SUPER_ADMIN.
     *
     * @throws Exception если произошла ошибка при выполнении запроса
     */
    @Test
    @WithMockUser(username = TRACKER, roles = "TRACKER")
    void getUserInfoNotSuperAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/users/{username}/info", TRACKER))
                .andExpect(status().isForbidden());
    }

    /**
     * Тест успешного получения списка трекеров.
     *
     * @throws Exception если произошла ошибка при выполнении запроса
     */
    @Test
    @WithMockUser(username = "superadmin", roles = "SUPER_ADMIN")
    void findAllTrackersSuccess() throws Exception {
        mockMvc.perform(post("/api/v1/users/trackers")
                .contentType("application/json")
                .content("""
                    {"filters": []}
                    """)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/json"))
                .andExpect(jsonPath("$.content[*].username")
                        .value(hasItem(TRACKER)))
                .andExpect(jsonPath("$.content[*].username")
                        .value(hasItem(RONIN)))
                .andExpect(jsonPath("$.page.totalElements").value(2));
    }

    /**
     * Тест получения списка трекеров с фильтрацией.
     *
     * @throws Exception если произошла ошибка при выполнении запроса
     */
    @Test
    @WithMockUser(username = "superadmin", roles = "SUPER_ADMIN")
    void findAllTrackersWithFiltersSuccess() throws Exception {
        mockMvc.perform(post("/api/v1/users/trackers")
                .contentType("application/json")
                .content("""
                    {"filters": [{"fieldName": "username",
                        "type": "EQ", "value": "tracker"}]}
                    """)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/json"))
                .andExpect(jsonPath("$.content[0].username")
                        .value(TRACKER));
    }

    /**
     * Тест получения списка трекеров с некорректными фильтрами.
     *
     * @throws Exception если произошла ошибка при выполнении запроса
     */
    @Test
    @WithMockUser(username = "superadmin", roles = "SUPER_ADMIN")
    void findAllTrackersWithFiltersBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/users/trackers")
                .contentType("application/json")
                .content("""
                    {"filters": [{"fieldName": "username123",
                        "type": "EQ", "value": "notfound"}]}
                    """)
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    /**
     * Тест получения списка трекеров без прав SUPER_ADMIN.
     *
     * @throws Exception если произошла ошибка при выполнении запроса
     */
    @Test
    @WithMockUser(username = TRACKER, roles = "TRACKER")
    void findAllTrackersNotSuperAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/users/trackers")
                .contentType("application/json")
                .content("""
                    {"filters": []}
                    """)
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    /**
     * Тест успешного получения списка администраторов.
     *
     * @throws Exception если произошла ошибка при выполнении запроса
     */
    @Test
    @WithMockUser(username = "superadmin", roles = "SUPER_ADMIN")
    void findAllAdminsSuccess() throws Exception {
        mockMvc.perform(post("/api/v1/users/administrators")
                .contentType("application/json")
                .content("""
                    {"filters": []}
                    """)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/json"))
                .andExpect(jsonPath("$.content[0].username")
                        .value("admin"));
    }

    /**
     * Тест успешной разблокировки пользователя.
     *
     * @throws Exception если произошла ошибка при выполнении запроса
     */
    @Test
    @WithMockUser(username = "superadmin", roles = "SUPER_ADMIN")
    void unlockUserSuccess() throws Exception {
        mockMvc.perform(post("/api/v1/users/disable")
                .param("username", TRACKER)
                .with(csrf()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/users/disable")
                .param("username", TRACKER)
                .with(csrf()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/users/unlock")
                .param("username", TRACKER)
                .with(csrf()))
                .andExpect(status().isOk());

        assertThat(userService.findByUsername(TRACKER)
                .getAccountNonLocked()).isTrue();
    }

    /**
     * Тест разблокировки пользователя без прав SUPER_ADMIN.
     *
     * @throws Exception если произошла ошибка при выполнении запроса
     */
    @Test
    @WithMockUser(username = TRACKER, roles = "TRACKER")
    void unlockUserNotSuperAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/users/unlock")
                .param("username", TRACKER)
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    /**
     * Тест успешного получения команд пользователя.
     *
     * @throws Exception если произошла ошибка при выполнении запроса
     */
    @Test
    @WithMockUser(username = "superadmin", roles = "SUPER_ADMIN")
    void getUserTeamsSuccess() throws Exception {
        mockMvc.perform(get("/api/v1/users/{username}/teams", TRACKER))
                .andExpect(status().isOk());
    }

    /**
     * Тест получения команд без прав SUPER_ADMIN.
     *
     * @throws Exception если произошла ошибка при выполнении запроса
     */
    @Test
    @WithMockUser(username = TRACKER, roles = "TRACKER")
    void getUserTeamsNotSuperAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/users/{username}/teams", TRACKER))
                .andExpect(status().isForbidden());
    }

    /**
     * Тест удаления несуществующего пользователя.
     *
     * @throws Exception если произошла ошибка при выполнении запроса
     */
    @Test
    @WithMockUser(username = "superadmin", roles = "SUPER_ADMIN")
    void deleteUserNotFound() throws Exception {
        mockMvc.perform(delete("/api/v1/users")
                .param("username", "nonexistentuser")
                .with(csrf()))
                .andExpect(status().isNotFound());
    }

    /**
     * Тест удаления пользователя без прав SUPER_ADMIN.
     *
     * @throws Exception если произошла ошибка при выполнении запроса
     */
    @Test
    @WithMockUser(username = TRACKER, roles = "TRACKER")
    void deleteUserNotSuperAdmin() throws Exception {
        mockMvc.perform(delete("/api/v1/users")
                .param("username", TRACKER)
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    /**
     * Тест успешного удаления пользователя.
     *
     * @throws Exception если произошла ошибка при выполнении запроса
     */
    @Test
    @WithMockUser(username = "superadmin", roles = "SUPER_ADMIN")
    void deleteUserSuccess() throws Exception {
        var dto = RegistrationRequestDto.builder()
                .username("todeletectrl")
                .password("Password@123")
                .phoneNumber("+1234567890")
                .fullName("To Delete Ctrl")
                .email("todeletectrl@test.com")
                .role("ADMIN")
                .build();
        userService.saveUser(dto);

        assertThat(userService.findByUsername("todeletectrl"))
                .isNotNull();

        mockMvc.perform(delete("/api/v1/users")
                .param("username", "todeletectrl")
                .with(csrf()))
                .andExpect(status().isOk());

        assertThatThrownBy(() ->
                userService.findByUsername("todeletectrl"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    /**
     * Тест разблокировки несуществующего пользователя.
     *
     * @throws Exception если произошла ошибка при выполнении запроса
     */
    @Test
    @WithMockUser(username = "superadmin", roles = "SUPER_ADMIN")
    void unlockUserNotFound() throws Exception {
        mockMvc.perform(post("/api/v1/users/unlock")
                .param("username", "nonexistentuser")
                .with(csrf()))
                .andExpect(status().isNotFound());
    }

    /**
     * Тест получения администраторов без прав SUPER_ADMIN.
     *
     * @throws Exception если произошла ошибка при выполнении запроса
     */
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getAdminsNotSuperAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/users/administrators")
                .contentType("application/json")
                .content("""
                    {"filters": []}
                    """)
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    /**
     * Тест обработки исключения при переназначении команды.
     * Ожидается статус 409 Conflict.
     *
     * @throws Exception если произошла ошибка при выполнении запроса
     */
    @Test
    @WithMockUser(username = "superadmin", roles = "SUPER_ADMIN")
    void handleTeamReassignmentExceptionReturns409() throws Exception {
        when(backendClient.getUserTeams(eq(RONIN), anyString()))
                .thenReturn(java.util.List.of(
                        java.util.Map.of("id", "1")));

        mockMvc.perform(post("/api/v1/users/enable")
                .param("username", RONIN)
                .with(csrf()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/users/disable")
                .param("username", RONIN)
                .with(csrf()))
                .andDo(print())
                .andExpect(status().is(HTTP_CONFLICT))
                .andExpect(jsonPath("$.error")
                        .value("TEAM_OPERATION_FAILED"))
                .andExpect(jsonPath("$.status").value(HTTP_CONFLICT));
    }

    /**
     * Тест успешного удаления пользователя Ronin.
     *
     * @throws Exception если произошла ошибка при выполнении запроса
     */
    @Test
    @WithMockUser(username = "superadmin", roles = "SUPER_ADMIN")
    void deleteUserRoninSuccess() throws Exception {
        mockMvc.perform(delete("/api/v1/users")
                .param("username", RONIN)
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk());
    }
}
