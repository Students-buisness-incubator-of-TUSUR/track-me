package net.trackme.meetingservice.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
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
public class OpenAPIConfiguration {

    public static final String CSRF_TOKEN = "X-CSRF-TOKEN";
    private final AppProperties appProperties;

    private final BuildProperties buildProperties;

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title(buildProperties.getName())
                                .description("API сервиса управления встречами TrackMe")
                                .version(buildProperties.getVersion()))
                .addSecurityItem(new SecurityRequirement().addList(CSRF_TOKEN))
                .components(new Components()
                        .addSecuritySchemes(
                                CSRF_TOKEN, new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER).name(CSRF_TOKEN)))
                .servers(List.of(new Server().url(appProperties.getApiUrl())));
    }
}