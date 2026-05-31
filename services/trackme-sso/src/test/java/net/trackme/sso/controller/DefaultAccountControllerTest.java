package net.trackme.sso.controller;

import net.trackme.sso.AbstractIntegrationTest;
import net.trackme.sso.dao.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционные тесты для контроллера управления аккаунтом.
 * Проверяет получение информации о пользователе и смену пароля.
 */
class DefaultAccountControllerTest extends AbstractIntegrationTest {

    /** MockMvc для выполнения HTTP-запросов в тестах. */
    @Autowired
    private MockMvc mockMvc;

    /** Репозиторий пользователей для подготовки тестовых данных. */
    @Autowired
    private UserRepository userRepository;

    /**
     * Подготовка тестовых данных перед каждым тестом.
     * Активирует пользователя superadmin, если он неактивен.
     */
    @BeforeEach
    void setUp() {
        userRepository.findByUsername("superadmin").ifPresent(admin -> {
            if (!admin.getActive()) {
                admin.setActive(true);
                userRepository.save(admin);
            }
        });
    }

    /**
     * Тест успешного получения информации о пользователе.
     * Ожидается статус 200 и корректные данные в ответе.
     *
     * @throws Exception если произошла ошибка при выполнении запроса
     */
    @Test
    @WithMockUser(username = "superadmin", roles = "SUPER_ADMIN")
    void getUserInfo_success() throws Exception {
        mockMvc.perform(get("/api/v1/account/info"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("superadmin"))
                .andExpect(jsonPath("$.email").value(""));
    }

    /**
     * Тест получения информации без аутентификации.
     * Ожидается статус 401 Unauthorized.
     *
     * @throws Exception если произошла ошибка при выполнении запроса
     */
    @Test
    @WithAnonymousUser
    void getUserInfo_unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/account/info"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    /**
     * Тест получения информации для несуществующего пользователя.
     * Ожидается статус 404 Not Found.
     *
     * @throws Exception если произошла ошибка при выполнении запроса
     */
    @Test
    @WithMockUser(username = "user", roles = "TRACKER")
    void getUserInfo_notFound() throws Exception {
        mockMvc.perform(get("/api/v1/account/info"))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    /**
     * Тест успешной смены пароля.
     * Ожидается статус 200 OK.
     *
     * @throws Exception если произошла ошибка при выполнении запроса
     */
    @Test
    @WithMockUser(username = "superadmin", roles = "SUPER_ADMIN")
    void changePassword_success() throws Exception {
        mockMvc.perform(post(
                "/api/v1/account/changePassword"
                        + "?newPassword=<PASSWORD>&oldPassword=superadmin")
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    /**
     * Тест смены пароля с неверным старым паролем.
     * Ожидается статус 400 Bad Request.
     *
     * @throws Exception если произошла ошибка при выполнении запроса
     */
    @Test
    @WithMockUser(username = "superadmin", roles = "SUPER_ADMIN")
    void changePassword_invalidOldPassword() throws Exception {
        mockMvc.perform(post(
                "/api/v1/account/changePassword"
                        + "?newPassword=<PASSWORD>&oldPassword=wrong")
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}
