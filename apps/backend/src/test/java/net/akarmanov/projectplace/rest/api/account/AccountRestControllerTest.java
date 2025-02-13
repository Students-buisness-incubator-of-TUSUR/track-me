package net.akarmanov.projectplace.rest.api.account;

import net.akarmanov.projectplace.BaseApplicationTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser(BaseApplicationTest.USERNAME)
class AccountRestControllerTest extends BaseApplicationTest {

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Test
  void getCurrentUserInfo_success() throws Exception {
    mockMvc.perform(get("/api/v1/users/current/info"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(user.getId().toString())));
  }

  @Test
  void updateUserInfo_success() throws Exception {
    mockMvc.perform(post("/api/v1/users/current/update")
            .contentType("application/json")
            .content("""
                {
                  "firstName": "Jane",
                  "lastName": "Doe",
                  "middleName": "Middle",
                  "phoneNumber": "+71234567890",
                  "telegramId": "telegramId"
                }
                """))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.firstName", is("Jane")))
        .andExpect(jsonPath("$.lastName", is("Doe")))
        .andExpect(jsonPath("$.middleName", is("Middle")))
        .andExpect(jsonPath("$.phoneNumber", is("+71234567890")))
        .andExpect(jsonPath("$.telegramId", is("telegramId")));
  }

  @Test
  void changePassword_success() throws Exception {
    var newPassword = "newPassword";
    mockMvc.perform(post("/api/v1/users/current/changePassword")
            .contentType("application/json")
            .param("oldPassword", PASSWORD)
            .param("newPassword", newPassword))
        .andDo(print())
        .andExpect(status().isNoContent());

    user = userRepository.findById(user.getId()).orElseThrow();

    Assertions.assertTrue(passwordEncoder.matches(newPassword, user.getPassword()));
  }
}