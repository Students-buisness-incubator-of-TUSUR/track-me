package net.akarmanov.projectplace.sso.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "net.akarmanov.projectplace.sso.dao.repository")
public class AppConfiguration {
  @Bean
  OpenAPI openApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("TrackMe SSO Service")
                .version("1.0.0")
                .description("Single Sign-On service for TrackMe application"));
  }
}
