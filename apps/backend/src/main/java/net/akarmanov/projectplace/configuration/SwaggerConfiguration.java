package net.akarmanov.projectplace.configuration;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityScheme.Type;
import io.swagger.v3.oas.models.servers.Server;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.models.GroupedOpenApi;
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
public class SwaggerConfiguration {

  public static final String BEARER_AUTH = "bearerAuth";

  private final AppProperties appProperties;

  @Bean
  public GroupedOpenApi baseOpenAPI() {
    return GroupedOpenApi.builder()
        .group("main")
        .pathsToMatch("/api/v1/**")
        .pathsToExclude("/api/v1/admin/**")
        .addOpenApiCustomizer(openAPI -> openAPI
            .info(new Info().title("Project Place API").version("1.0"))
            .schemaRequirement(BEARER_AUTH, new SecurityScheme()
                .type(Type.HTTP)
                .scheme("bearer")
                .name("Authorization")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER))
            .servers(List.of(
                new Server().url(appProperties.getApiUrl())))
            .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH)))
        .build();
  }

  @Bean
  public GroupedOpenApi adminAPI() {
    return GroupedOpenApi.builder()
        .group("admin")
        .pathsToMatch("/api/v1/admin/**")
        .pathsToMatch("/api/v1/auth/**")
        .packagesToScan("net.akarmanov.projectplace.rest.api.admin")
        .addOpenApiCustomizer(openAPI -> openAPI
            .info(new Info().title("Project Place Admin API").version("1.0"))
            .schemaRequirement(BEARER_AUTH, new SecurityScheme()
                .type(Type.HTTP)
                .scheme("bearer")
                .name("Authorization")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER))
            .servers(List.of(
                new Server().url(appProperties.getApiUrl())))
            .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH)))
        .build();
  }
}