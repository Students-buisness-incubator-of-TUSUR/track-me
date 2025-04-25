package net.akarmanov.projectplace.cliengateway;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

@SpringBootTest
class ClientGatewayApplicationTest {

  @Autowired
  private Environment environment;

  @Test
  void contextLoads() {
    Assertions.assertThat(environment).isNotNull();
  }

}