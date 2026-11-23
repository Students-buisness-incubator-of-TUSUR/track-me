package net.trackme.sso.controller;

import net.trackme.sso.AbstractIntegrationTest;
import net.trackme.sso.dao.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DefaultAccountControllerTest extends AbstractIntegrationTest {
  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @BeforeEach
  void setUp() {
    // Интеграционные тесты используют общую БД, поэтому приводим superadmin
    // к состоянию, от которого зависят проверки этого класса.
    userRepository.findByUsername("superadmin").ifPresent(admin -> {
      admin.setActive(true);
      admin.setEmail("");
      admin.setPasswordHash(passwordEncoder.encode("superadmin"));
      userRepository.save(admin);
    });
  }

  @Test
  @WithMockUser(username = "superadmin", roles = "SUPER_ADMIN")
  void getUserInfo_success() throws Exception {
    mockMvc.perform(get("/api/v1/account/info"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("superadmin"))
        .andExpect(jsonPath("$.email").value(""));
  }

  @Test
  @WithAnonymousUser
  void getUserInfo_unauthorized() throws Exception {
    mockMvc.perform(get("/api/v1/account/info"))
        .andDo(print())
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(username = "user", roles = "TRACKER")
  void getUserInfo_notFound() throws Exception {
    mockMvc.perform(get("/api/v1/account/info"))
        .andDo(print())
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(username = "superadmin", roles = "SUPER_ADMIN")
  void changePassword_success() throws Exception {
    mockMvc.perform(post(
            "/api/v1/account/changePassword?newPassword=<PASSWORD>&oldPassword=superadmin")
            .with(csrf()))
        .andDo(print())
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(username = "superadmin", roles = "SUPER_ADMIN")
  void changePassword_invalidOldPassword() throws Exception {
    mockMvc.perform(post("/api/v1/account/changePassword?newPassword=<PASSWORD>&oldPassword=wrong")
            .with(csrf()))
        .andDo(print())
        .andExpect(status().isBadRequest());
  }
}