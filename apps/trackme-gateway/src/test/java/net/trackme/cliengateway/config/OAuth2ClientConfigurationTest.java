package net.trackme.cliengateway.config;

import net.trackme.cliengateway.ClientGatewayApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.reactive.ReactiveOAuth2ClientAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = ClientGatewayApplication.class)
@EnableAutoConfiguration(exclude = {ReactiveOAuth2ClientAutoConfiguration.class})
class OAuth2ClientConfigurationTest {
    @Autowired
    private Environment environment;

    @MockitoBean
    private ReactiveClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private ServerOAuth2AuthorizedClientRepository clientRepository;

    @Test
    void testContextLoads() {
        assertNotNull(environment, "Environment should not be null");
        assertNotNull(clientRegistrationRepository, "ClientRegistrationRepository should not be null");
        assertNotNull(clientRepository, "ServerOAuth2AuthorizedClientRepository should not be null");
    }
}