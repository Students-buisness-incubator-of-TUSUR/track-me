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

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties({AppProperties.class})
@EnableJpaRepositories(basePackages = "net.trackme.sso.dao.repository")
public class AppConfiguration {

    public static final String XSRF_TOKEN = "X-CSRF-TOKEN";
    private final AppProperties appProperties;

    private final BuildProperties buildProperties;


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

    @Bean
    public RestClient backendRestClient() {
        return RestClient.create(appProperties.getServices().getBackend().getUrl());
    }
}
