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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DefaultUserControllerTest extends AbstractIntegrationTest {

  private static final String TRACKER = "tracker";
  private static final String RONIN = "ronin";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserDetailsService userDetailsService;

  @Autowired
  private UserService userService;

  @MockitoBean
  private BackendClient backendClient;

  @BeforeEach
  void setUp() {
    reset(backendClient);
    when(backendClient.getUserTeams(anyString(), anyString()))
        .thenReturn(java.util.Collections.emptyList());
    doNothing().when(backendClient).reassignTeamsToRonin(any(), anyString());

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

  @Test
  @WithMockUser(username = "superadmin", roles = "SUPER_ADMIN")
  void enableUser_success() throws Exception {

    mockMvc.perform(post("/api/v1/users/enable")
            .param("username", TRACKER)
            .with(csrf()))
        .andExpect(status().isOk());

    assertThat(userDetailsService.loadUserByUsername(TRACKER).isEnabled()).isTrue();

    mockMvc.perform(post("/api/v1/users/disable")
            .param("username", TRACKER)
            .with(csrf()))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(username = "superadmin", roles = "SUPER_ADMIN")
  void disableUser_success() throws Exception {

    mockMvc.perform(post("/api/v1/users/enable")
            .param("username", TRACKER)
            .with(csrf()))
        .andExpect(status().isOk());

    assertThat(userDetailsService.loadUserByUsername(TRACKER).isEnabled()).isTrue();

    mockMvc.perform(post("/api/v1/users/disable")
            .param("username", TRACKER)
            .with(csrf()))
        .andExpect(status().isOk());

    assertThat(userDetailsService.loadUserByUsername(TRACKER).isEnabled()).isFalse();

    mockMvc.perform(post("/api/v1/users/disable")
           .param("username", TRACKER)
           .with(csrf()))
        .andExpect(status().isOk());

    assertThat(userService.findByUsername(TRACKER).getAccountNonLocked()).isFalse();

    mockMvc.perform(post("/api/v1/users/enable")
            .param("username", TRACKER)
            .with(csrf()))
        .andExpect(status().isOk());
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
  @WithMockUser(username = TRACKER, roles = "TRACKER")
  void enableUser_notSuperAdmin() throws Exception {
    mockMvc.perform(post("/api/v1/users/enable")
            .param("username", TRACKER)
            .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(username = TRACKER, roles = "TRACKER")
  void disableUser_notSuperAdmin() throws Exception {
    mockMvc.perform(post("/api/v1/users/disable")
            .param("username", TRACKER)
            .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(username = "superadmin", roles = "SUPER_ADMIN")
  void getUserInfo_success() throws Exception {
    mockMvc.perform(get("/api/v1/users/{username}/info", TRACKER))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", "application/json"))
        .andExpect(jsonPath("$.username").value(TRACKER));
  }

  @Test
  @WithMockUser(username = TRACKER, roles = "TRACKER")
  void getUserInfo_notSuperAdmin() throws Exception {
    mockMvc.perform(get("/api/v1/users/{username}/info", TRACKER))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(username = "superadmin", roles = "SUPER_ADMIN")
  void findAllTrackers_success() throws Exception {
    mockMvc.perform(post("/api/v1/users/trackers")
            .contentType("application/json")
            .content("""
                {"filters": []}
                """)
            .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", "application/json"))
        .andExpect(jsonPath("$.content[*].username").value(hasItem(TRACKER)))
        .andExpect(jsonPath("$.content[*].username").value(hasItem(RONIN)))
        .andExpect(jsonPath("$.page.totalElements").value(2));
  }

  @Test
  @WithMockUser(username = "superadmin", roles = "SUPER_ADMIN")
  void findAllTrackers_withFilters_success() throws Exception {
    mockMvc.perform(post("/api/v1/users/trackers")
            .contentType("application/json")
            .content("""
                {"filters": [{"fieldName": "username", "type": "EQ", "value": "tracker"}]}
                """)
            .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", "application/json"))
        .andExpect(jsonPath("$.content[0].username").value(TRACKER));
  }

  @Test
  @WithMockUser(username = "superadmin", roles = "SUPER_ADMIN")
  void findAllTrackers_withFilters_badRequest() throws Exception {
    mockMvc.perform(post("/api/v1/users/trackers")
            .contentType("application/json")
            .content("""
                {"filters": [{"fieldName": "username123", "type": "EQ", "value": "notfound"}]}
                """)
            .with(csrf()))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(username = TRACKER, roles = "TRACKER")
  void findAllTrackers_notSuperAdmin() throws Exception {
    mockMvc.perform(post("/api/v1/users/trackers")
            .contentType("application/json")
            .content("""
                {"filters": []}
                """)
            .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(username = "superadmin", roles = "SUPER_ADMIN")
  void findAllAdmins_success() throws Exception {
    mockMvc.perform(post("/api/v1/users/administrators")
            .contentType("application/json")
            .content("""
                {"filters": []}
                """)
            .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", "application/json"))
        .andExpect(jsonPath("$.content[0].username").value("admin"));
  }

  @Test
  @WithMockUser(username = "superadmin", roles = "SUPER_ADMIN")
  void unlockUser_success() throws Exception {
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

    assertThat(userService.findByUsername(TRACKER).getAccountNonLocked()).isTrue();
  }

  @Test
  @WithMockUser(username = TRACKER, roles = "TRACKER")
  void unlockUser_notSuperAdmin() throws Exception {
    mockMvc.perform(post("/api/v1/users/unlock")
            .param("username", TRACKER)
            .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(username = "superadmin", roles = "SUPER_ADMIN")
  void getUserTeams_success() throws Exception {
    mockMvc.perform(get("/api/v1/users/{username}/teams", TRACKER))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(username = TRACKER, roles = "TRACKER")
  void getUserTeams_notSuperAdmin() throws Exception {
    mockMvc.perform(get("/api/v1/users/{username}/teams", TRACKER))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(username = "superadmin", roles = "SUPER_ADMIN")
  void deleteUser_notFound() throws Exception {
    mockMvc.perform(delete("/api/v1/users")
            .param("username", "nonexistentuser")
            .with(csrf()))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(username = TRACKER, roles = "TRACKER")
  void deleteUser_notSuperAdmin() throws Exception {
    mockMvc.perform(delete("/api/v1/users")
            .param("username", TRACKER)
            .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(username = "superadmin", roles = "SUPER_ADMIN")
  void deleteUser_success() throws Exception {
        var dto = RegistrationRequestDto.builder()
            .username("todeletectrl")
            .password("Password@123")
            .phoneNumber("+1234567890")
            .fullName("To Delete Ctrl")
            .email("todeletectrl@test.com")
            .role("ADMIN")
            .build();
        userService.saveUser(dto);
        
        assertThat(userService.findByUsername("todeletectrl")).isNotNull();
        
        mockMvc.perform(delete("/api/v1/users")
                .param("username", "todeletectrl")
                .with(csrf()))
            .andExpect(status().isOk());
        
        assertThatThrownBy(() -> userService.findByUsername("todeletectrl"))
            .isInstanceOf(UsernameNotFoundException.class);
  }

  @Test
  @WithMockUser(username = "superadmin", roles = "SUPER_ADMIN")
  void unlockUser_notFound() throws Exception {
      mockMvc.perform(post("/api/v1/users/unlock")
              .param("username", "nonexistentuser")
              .with(csrf()))
          .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(username = "admin", roles = "ADMIN")
  void getAdmins_notSuperAdmin() throws Exception {
      mockMvc.perform(post("/api/v1/users/administrators")
              .contentType("application/json")
              .content("""
                  {"filters": []}
                  """)
              .with(csrf()))
          .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(username = "superadmin", roles = "SUPER_ADMIN")
  void handleTeamReassignmentException_returns409() throws Exception {
    when(backendClient.getUserTeams(eq(RONIN), anyString()))
        .thenReturn(java.util.List.of(java.util.Map.of("id", "1")));

    mockMvc.perform(post("/api/v1/users/enable")
            .param("username", RONIN)
            .with(csrf()))
        .andExpect(status().isOk());

    mockMvc.perform(post("/api/v1/users/disable")
            .param("username", RONIN)
            .with(csrf()))
        .andDo(print())
        .andExpect(status().is(409))
        .andExpect(jsonPath("$.error").value("TEAM_OPERATION_FAILED"))
        .andExpect(jsonPath("$.status").value(409));
  }

  @Test
  @WithMockUser(username = "superadmin", roles = "SUPER_ADMIN")
  void deleteUser_ronin_success() throws Exception {
    mockMvc.perform(delete("/api/v1/users")
            .param("username", RONIN)
            .with(csrf()))
        .andDo(print())
        .andExpect(status().isOk());
  }
}
