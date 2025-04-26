package net.akarmanov.projectplace.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Конфигурация Swagger.
 *
 * @see <a href="https://springdoc.org">SpringDoc</a>
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(AppProperties.class)
public class SwaggerConfiguration {

  private final AppProperties appProperties;

  private final BuildProperties buildProperties;

  @Bean
  public OpenAPI openApi() {
    return new OpenAPI()
        .info(new Info()
            .title(buildProperties.getName())
            .version(buildProperties.getVersion()))
        .servers(List.of(new Server()
            .url(appProperties.getApiUrl())));
  }
}