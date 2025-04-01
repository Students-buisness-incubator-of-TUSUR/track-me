package net.akarmanov.projectplace.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
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

  private final AppProperties appProperties;

  @Bean
  public GroupedOpenApi adminAPI() {
    return GroupedOpenApi.builder()
        .group("backend")
        .addOpenApiCustomizer(openAPI -> openAPI
            .info(new Info().title("TrackMe API").version("1.0"))
            .servers(List.of(
                new Server().url(appProperties.getApiUrl())))
            .addSecurityItem(new SecurityRequirement().addList("oauth2Scheme"))
            .setComponents(new Components()
                .addSecuritySchemes("oauth2Scheme", new SecurityScheme()
                    .type(Type.OAUTH2)
                    .flows(new OAuthFlows()
                        .authorizationCode(new OAuthFlow()
                            .tokenUrl(appProperties.getApiUrl() + "/oauth2/token")
                            .authorizationUrl(appProperties.getApiUrl() + "/oauth2/authorize")
                            .refreshUrl(appProperties.getApiUrl() + "/oauth2/refresh")
                            .scopes(new Scopes()
                                .addString("openid", "openid")))
                        .clientCredentials(new OAuthFlow()
                            .tokenUrl(appProperties.getApiUrl() + "/oauth2/token")
                            .scopes(new Scopes()
                                .addString("openid", "openid")))))))
        .build();
  }
}