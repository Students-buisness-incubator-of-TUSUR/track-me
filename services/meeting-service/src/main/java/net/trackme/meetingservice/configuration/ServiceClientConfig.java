package net.trackme.meetingservice.configuration;

import lombok.RequiredArgsConstructor;
import net.trackme.meetingservice.services.integration.SecurityPropagationInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class ServiceClientConfig {

    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService) {

        var provider = OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build();

        var manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                clientRegistrationRepository, authorizedClientService);
        manager.setAuthorizedClientProvider(provider);
        return manager;
    }

    @Bean("userRestClient")
    public RestClient userRestClient(RestClient.Builder builder, SecurityPropagationInterceptor interceptor) {
        return builder.requestInterceptor(interceptor).build();
    }

    @Bean("serviceRestClient")
    public RestClient serviceRestClient(RestClient.Builder builder, OAuth2AuthorizedClientManager manager) {
        var interceptor = new OAuth2ClientHttpRequestInterceptor(manager);
        interceptor.setClientRegistrationIdResolver(req -> "trackme-service");
        return builder.requestInterceptor(interceptor).build();
    }
}