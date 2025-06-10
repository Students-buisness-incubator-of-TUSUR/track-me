package net.trackme.sso;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc(print = MockMvcPrint.DEFAULT)
@Testcontainers
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {
  private static final PostgreSQLContainer<?> POSTGRESQL_CONTAINER =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withReuse(true)
          .withDatabaseName("test")
          .withUsername("test")
          .withPassword("test")
          .withInitScript("init-test-schema.sql");

  private static final GenericContainer<?> REDIS_CONTAINER =
      new GenericContainer<>("redis:7.0.11-alpine")
          .withReuse(true)
          .waitingFor(Wait.forListeningPort())
          .withExposedPorts(6379);

  static {
    POSTGRESQL_CONTAINER.start();
    REDIS_CONTAINER.start();
  }


  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRESQL_CONTAINER::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRESQL_CONTAINER::getUsername);
    registry.add("spring.datasource.password", POSTGRESQL_CONTAINER::getPassword);

    registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
    registry.add("spring.data.redis.port", () -> REDIS_CONTAINER.getMappedPort(6379).toString());
  }

  @Test
  void contextLoads() {
    assertThat(POSTGRESQL_CONTAINER.isRunning()).isTrue();
    assertThat(REDIS_CONTAINER.isRunning()).isTrue();
  }
}
