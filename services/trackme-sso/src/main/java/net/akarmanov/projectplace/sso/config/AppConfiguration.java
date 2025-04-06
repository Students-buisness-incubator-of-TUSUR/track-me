package net.akarmanov.projectplace.sso.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@RequiredArgsConstructor
@EnableJpaRepositories(basePackages = "net.akarmanov.projectplace.sso.dao.repository")
public class AppConfiguration {

  private final BuildProperties buildProperties;

  @Bean
  OpenAPI openApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("TrackMe SSO Service")
                .version(buildProperties.getVersion())
                .description("Единая точка входа для всех сервисов системы TrackMe"));
  }
}
