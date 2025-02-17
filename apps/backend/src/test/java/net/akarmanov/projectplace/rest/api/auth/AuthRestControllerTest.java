package net.akarmanov.projectplace.rest.api.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.akarmanov.projectplace.BaseApplicationTest;
import net.akarmanov.projectplace.domain.User;
import net.akarmanov.projectplace.models.UserRole;
import net.akarmanov.projectplace.repos.UserRepository;
import net.akarmanov.projectplace.rest.api.dto.SingInRequest;
import net.akarmanov.projectplace.rest.api.dto.SingUpRequest;
import net.akarmanov.projectplace.rest.api.dto.UserRoleDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(
    properties = {
        "JWT_SECRET=12345678905675675674564564566756756756745645656"
    })
class AuthRestControllerTest extends BaseApplicationTest {

  public static final String USERNAME = "username";

  public static final String PASSWORD = "password";

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    userRepository.save(User.builder()
        .password(passwordEncoder.encode(PASSWORD))
        .phoneNumber("+71234567890")
        .telegramId(USERNAME)
        .fullName("John Doe")
        .enabled(true)
        .role(UserRole.ADMIN)
        .build());
  }

  @AfterEach
  void tearDown() {
    userRepository.deleteAllInBatch();
  }

  @Test
  void testSignIn_success() throws Exception {
    var signIn = SingInRequest.builder()
        .telegramId(USERNAME)
        .password(PASSWORD)
        .build();
    mockMvc.perform(post("/api/v1/auth/sing-in")
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(signIn)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON));
  }

  @Test
  void testSignIn_failure() throws Exception {
    var signIn = SingInRequest.builder()
        .telegramId(USERNAME)
        .password("wrongPassword")
        .build();
    mockMvc.perform(post("/api/v1/auth/sing-in")
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(signIn)))
        .andDo(print())
        .andExpect(status().isUnauthorized());
  }

  @Test
  void testSignUp_success() throws Exception {
    var signUp = SingUpRequest.builder()
        .password("newPassword")
        .phoneNumber("+71234567891")
        .telegramId("newTelegramId")
        .fullName("John Doe")
        .role(UserRoleDto.ADMIN)
        .build();
    mockMvc.perform(post("/api/v1/auth/sing-up")
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(signUp)))
        .andDo(print())
        .andExpect(status().isOk());
  }

  @Test
  void testSignUp_failure() throws Exception {
    var signUp = SingUpRequest.builder()
        .password(PASSWORD)
        .phoneNumber("+71234567892")
        .role(UserRoleDto.ADMIN)
        .fullName("John Doe")
        .build();
    mockMvc.perform(post("/api/v1/auth/sing-up")
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(signUp)))
        .andDo(print())
        .andExpect(status().isBadRequest());
  }
}