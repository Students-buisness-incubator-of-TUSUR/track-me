package net.trackme.sso.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Главный класс конфигурации для сервиса TrackMe SSO.
 * Настраивает документацию Swagger/OpenAPI, репозитории JPA
 * и REST-клиент для взаимодействия с бэкендом.
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties({AppProperties.class})
@EnableJpaRepositories(basePackages = "net.trackme.sso.dao.repository")
public class AppConfiguration {

    /** Название заголовка для токена CSRF-защиты. */
    public static final String XSRF_TOKEN = "X-CSRF-TOKEN";

    /** Свойства приложения. */
    private final AppProperties appProperties;

    /** Информация о сборке приложения. */
    private final BuildProperties buildProperties;

    /**
     * Создаёт и настраивает спецификацию OpenAPI для SSO-сервиса.
     * Настраивает схему безопасности с использованием CSRF-токена,
     * URL API-сервера и информацию о приложении из свойств сборки.
     *
     * @return настроенный экземпляр {@link OpenAPI}
     */
    @Bean
    OpenAPI openApi() {
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList(XSRF_TOKEN))
                .components(new Components().addSecuritySchemes(
                        XSRF_TOKEN,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name(XSRF_TOKEN)
                ))
                .servers(List.of(new io.swagger.v3.oas.models.servers.Server()
                        .url(appProperties.getApiUrl())))
                .info(new Info()
                        .title(buildProperties.getName())
                        .version(buildProperties.getVersion())
                        .description("Единая точка входа в сервисы TrackMe.")
                );
    }

    /**
     * Создаёт REST-клиент для взаимодействия с бэкенд-сервисом.
     * Клиент настраивается с URL бэкенда из свойств приложения.
     *
     * @return настроенный экземпляр {@link RestClient} для взаимодействия с бэкендом
     */
    @Bean
    public RestClient backendRestClient() {
        return RestClient.create(appProperties.getServices().getBackend().getUrl());
    }
}
