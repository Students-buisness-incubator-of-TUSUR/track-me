package net.akarmanov.projectplace;

import net.akarmanov.projectplace.domain.ReadinessLevel;
import net.akarmanov.projectplace.domain.Stream;
import net.akarmanov.projectplace.domain.User;
import net.akarmanov.projectplace.models.UserRole;
import net.akarmanov.projectplace.repos.StreamRepository;
import net.akarmanov.projectplace.repos.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(
    properties = {
        "JWT_SECRET=12345678905675675674564564566756756756745645656"
    })
@Sql(value = {"classpath:init-test-schema.sql"},
     executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@Sql(value = {"classpath:initial-data.sql"},
     executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(value = "classpath:cleanup.sql",
     executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
public abstract class BaseApplicationTest {

  public static final String USERNAME = "test_superadmin";

  public static final String PASSWORD = "123456";

  @Autowired
  protected MockMvc mockMvc;

  protected User user;

  @Autowired
  protected UserRepository userRepository;

  @Autowired
  protected StreamRepository streamRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @BeforeEach
  void initUser() {
    streamRepository.save(Stream.builder()
        .name("stream 1")
        .startDate(LocalDate.now())
        .endDate(LocalDate.now().plusDays(1))
        .readinessLevel(ReadinessLevel.LEVEL_1)
        .build());
    user = userRepository.save(User.builder()
        .enabled(true)
        .firstName("Иван")
        .lastName("Иванов")
        .telegramId(USERNAME)
        .email("")
        .password(passwordEncoder.encode(PASSWORD))
        .role(UserRole.SUPER_ADMIN)
        .build()
    );
  }

  @AfterEach
  void deleteUser() {
    userRepository.deleteAll();
    streamRepository.deleteAll();
  }
}
