package net.akarmanov.projectplace.sso.controller.impl;

import net.akarmanov.projectplace.sso.AbstractIntegrationTest;
import net.akarmanov.projectplace.sso.config.security.SecurityConfiguration;
import net.akarmanov.projectplace.sso.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DefaultUserControllerTest extends AbstractIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserDetailsService userDetailsService;

  @Autowired
  private UserService userService;

  @Test
  @WithMockUser(username = "superadmin", roles = "SUPER_ADMIN")
  void enableUser_success() throws Exception {

    mockMvc.perform(formLogin(SecurityConfiguration.LOGIN_PAGE)
            .user("tracker")
            .password("tracker"))
        .andExpect(status().isFound())
        .andExpect(header().string("Location", containsString("error")));

    mockMvc.perform(post("/api/v1/users/enable")
            .param("username", "tracker")
            .with(csrf()))
        .andExpect(status().isOk());

    mockMvc.perform(formLogin(SecurityConfiguration.LOGIN_PAGE)
            .user("tracker")
            .password("tracker"))
        .andExpect(status().isFound())
        .andExpect(header().string("Location", "/"));

    assertThat(userDetailsService.loadUserByUsername("tracker").isEnabled())
        .isTrue();
    userService.disableUser("tracker");
  }

  @Test
  @WithMockUser(username = "superadmin", roles = "SUPER_ADMIN")
  void disableUser_success() throws Exception {

    userService.enableUser("tracker");

    mockMvc.perform(formLogin(SecurityConfiguration.LOGIN_PAGE)
            .user("tracker")
            .password("tracker"))
        .andExpect(status().isFound())
        .andExpect(header().string("Location", not(containsString("error"))));

    mockMvc.perform(post("/api/v1/users/disable")
            .param("username", "tracker")
            .with(csrf()))
        .andExpect(status().isOk());

    mockMvc.perform(formLogin(SecurityConfiguration.LOGIN_PAGE)
            .user("tracker")
            .password("tracker"))
        .andExpect(status().isFound())
        .andExpect(header().string("Location", containsString("error")));

    assertThat(userDetailsService.loadUserByUsername("tracker").isEnabled())
        .isFalse();
  }

  @Test
  @WithMockUser(username = "superadmin", roles = "SUPER_ADMIN")
  void enableUser_notFound() throws Exception {
    mockMvc.perform(post("/api/v1/users/enable")
            .param("username", "notfound")
            .with(csrf()))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(username = "tracker", roles = "TRACKER")
  void enableUser_notSuperAdmin() throws Exception {
    mockMvc.perform(post("/api/v1/users/enable")
            .param("username", "tracker")
            .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(username = "tracker", roles = "TRACKER")
  void disableUser_notSuperAdmin() throws Exception {
    mockMvc.perform(post("/api/v1/users/disable")
            .param("username", "tracker")
            .with(csrf()))
        .andExpect(status().isForbidden());
  }

}