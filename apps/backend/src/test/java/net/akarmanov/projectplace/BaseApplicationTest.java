package net.akarmanov.projectplace;

import net.akarmanov.projectplace.domain.Stream;
import net.akarmanov.projectplace.repos.StreamRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc(print = MockMvcPrint.DEFAULT, printOnlyOnFailure = false)
@ActiveProfiles("test")
@TestPropertySource(
    properties = {
        "JWT_SECRET=12345678905675675674564564566756756756745645656"
    })
public abstract class BaseApplicationTest extends AbstractIntegrationTest {

  public static final String USER = "superadmin";

  @Autowired
  protected MockMvc mockMvc;

  @Autowired
  protected StreamRepository streamRepository;

  protected Stream stream;

  @BeforeEach
  void initUser() {
    stream = streamRepository.save(Stream.builder()
        .name("stream 1")
        .startDate(LocalDate.now())
        .active(true)
        .endDate(LocalDate.now().plusDays(1))
        .build());
  }

  @AfterEach
  void deleteUser() {
    streamRepository.deleteAllInBatch();
  }
}
