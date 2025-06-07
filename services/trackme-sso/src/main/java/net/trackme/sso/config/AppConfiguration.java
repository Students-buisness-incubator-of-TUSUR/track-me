package net.trackme.sso.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.ArrayList;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties({AppProperties.class})
@EnableJpaRepositories(basePackages = "net.trackme.sso.dao.repository")
public class AppConfiguration {

  private final AppProperties appProperties;

  private final BuildProperties buildProperties;


  @Bean
  OpenAPI openApi() {
    var swaggerProperties = appProperties.getSwagger();
    Components components = new Components();
    List<SecurityRequirement> securityRequirements = new ArrayList<>();

    // Добавим возможность OAuth2 Authorization code flow
    if (Boolean.TRUE.equals(swaggerProperties.getAuthTypes().getAuthorizationCodeEnabled())) {
      String securitySchemeName = "Authorization code flow";
      components.addSecuritySchemes(securitySchemeName, new SecurityScheme()
          .type(SecurityScheme.Type.OAUTH2)
          .flows(new OAuthFlows().authorizationCode(
              new OAuthFlow()
                  .tokenUrl(swaggerProperties.getAuthOauth().getTokenUrl())
                  .authorizationUrl(swaggerProperties.getAuthOauth().getAuthorizationUrl())
                  .refreshUrl(swaggerProperties.getAuthOauth().getRefreshUrl())
          )));
      securityRequirements.add(new SecurityRequirement().addList(securitySchemeName));
    }

    // Добавим возможность OAuth2 Client credentials flow
    if (Boolean.TRUE.equals(swaggerProperties.getAuthTypes().getClientCredentialsEnabled())) {
      String securitySchemeName = "Client credentials flow";
      components.addSecuritySchemes(securitySchemeName, new SecurityScheme()
          .type(SecurityScheme.Type.OAUTH2)
          .flows(new OAuthFlows().clientCredentials(
              new OAuthFlow().tokenUrl(swaggerProperties.getAuthOauth().getTokenUrl())
          )));
      securityRequirements.add(new SecurityRequirement().addList(securitySchemeName));
    }

    return new OpenAPI()
        .components(components)
        .security(securityRequirements)
        .servers(List.of(new io.swagger.v3.oas.models.servers.Server()
            .url(appProperties.getApiUrl())))
        .info(new Info()
            .title(buildProperties.getName())
            .version(buildProperties.getVersion())
            .description("Единая точка входа в сервисы TrackMe.")
        );
  }
}
