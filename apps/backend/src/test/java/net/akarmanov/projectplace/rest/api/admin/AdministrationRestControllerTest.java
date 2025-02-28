package net.akarmanov.projectplace.rest.api.admin;

import net.akarmanov.projectplace.BaseApplicationTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdministrationRestControllerTest extends BaseApplicationTest {

  @Test
  @WithMockUser(username = BaseApplicationTest.USERNAME, roles = {"SUPER_ADMIN"})
  void confirm_success() throws Exception {
    var user = userRepository.findByTelegramId("test_user").orElseThrow();

    mockMvc.perform(post("/api/v1/admin/users/confirm")
            .param("userId", user.getId().toString()))
        .andExpect(status().isOk());

    user = userRepository.findByTelegramId("test_user").orElseThrow();
    Assertions.assertTrue(user.isEnabled());
  }

  @Test
  @WithMockUser(username = BaseApplicationTest.USERNAME, roles = {"SUPER_ADMIN"})
  void unconfirm_success() throws Exception {
    var user = userRepository.findByTelegramId("test_user").orElseThrow();

    mockMvc.perform(post("/api/v1/admin/users/unconfirm")
            .param("userId", user.getId().toString()))
        .andExpect(status().isOk());

    user = userRepository.findByTelegramId("test_user").orElseThrow();
    Assertions.assertFalse(user.isEnabled());
  }

  @Test
  @WithMockUser(username = BaseApplicationTest.USERNAME, roles = {"SUPER_ADMIN"})
  void getTrackers_success() throws Exception {
    mockMvc.perform(post("/api/v1/admin/users/trackers")
            .contentType("application/json")
            .content("""
                {
                  "filters": []
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content.length()").value(2));
  }

  @Test
  @WithMockUser(username = BaseApplicationTest.USERNAME, roles = {"SUPER_ADMIN"})
  void getAdministrators_success() throws Exception {
    mockMvc.perform(post("/api/v1/admin/users/administrators")
            .contentType("application/json")
            .content("""
                {
                  "filters": []
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content.length()").value(1));
  }

  @Test
  @WithMockUser(username = BaseApplicationTest.USERNAME, roles = {"ADMIN"})
  void getAdministrators_accessDenied() throws Exception {
    mockMvc.perform(post("/api/v1/admin/users/administrators")
            .contentType("application/json")
            .content("""
                {
                  "filters": []
                }
                """))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(username = BaseApplicationTest.USERNAME, roles = {"SUPER_ADMIN"})
  void getUser_success() throws Exception {
    var user = userRepository.findByTelegramId("test_user").orElseThrow();

    mockMvc.perform(get("/api/v1/admin/users/{userId}/info", user.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(user.getId().toString()))
        .andExpect(jsonPath("$.fullName").value(user.getFullName()))
        .andExpect(jsonPath("$.telegramId").value(user.getTelegramId()))
        .andExpect(jsonPath("$.phoneNumber").value(user.getPhoneNumber()))
        .andExpect(jsonPath("$.email").value(user.getEmail()));
  }

  @Test
  @WithMockUser(username = BaseApplicationTest.USERNAME, roles = {"SUPER_ADMIN"})
  void getUser_notFound() throws Exception {
    mockMvc.perform(get("/api/v1/admin/users/{userId}/info",
            "00000000-0000-0000-0000-000000000000"))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(username = BaseApplicationTest.USERNAME, roles = {"SUPER_ADMIN"})
  void delete_success() throws Exception {
    var user = userRepository.findByTelegramId("test_user").orElseThrow();

    mockMvc.perform(post("/api/v1/admin/users/delete")
            .param("userId", user.getId().toString()))
        .andExpect(status().isOk());

    user = userRepository.findByTelegramId("test_user").orElseThrow();
    Assertions.assertTrue(user.isDeleted());

    mockMvc.perform(post("/api/v1/auth/sing-in")
            .contentType("application/json")
            .content("""
                {
                  "telegramId": "test_user",
                  "password": "superadmin"
                }
                """))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(username = BaseApplicationTest.USERNAME, roles = {"SUPER_ADMIN"})
  void undelete_success() throws Exception {
    var user = userRepository.findByTelegramId("test_admin").orElseThrow();

    mockMvc.perform(post("/api/v1/admin/users/undelete")
            .param("userId", user.getId().toString()))
        .andExpect(status().isOk());

    user = userRepository.findByTelegramId("test_admin").orElseThrow();
    Assertions.assertFalse(user.isDeleted());
  }
}