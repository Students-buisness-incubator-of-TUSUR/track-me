package net.trackme.cliengateway.config;

import net.trackme.cliengateway.ClientGatewayApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.reactive.ReactiveOAuth2ClientAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.cors.reactive.CorsWebFilter;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = ClientGatewayApplication.class)
@EnableAutoConfiguration(exclude = {ReactiveOAuth2ClientAutoConfiguration.class})
class CorsConfigurationTest {

    @Autowired
    private CorsWebFilter corsWebFilter;

    @Autowired
    private AppProperties appProperties;

    @MockitoBean
    private ReactiveClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private ServerOAuth2AuthorizedClientRepository clientRepository;

    @Test
    void testCorsWebFilterIsConfigured() {
        assertNotNull(corsWebFilter, "CorsWebFilter should be configured");
    }

    @Test
    void testCorsPropertiesAreLoaded() {
        assertNotNull(appProperties, "AppProperties should not be null");
        assertNotNull(appProperties.cors(), "CORS properties should not be null");

        var corsProperties = appProperties.cors();

        // Verify credentials are enabled
        assertTrue(corsProperties.allowCredentials(), "Allow credentials should be true");

        // Verify allowed methods are configured
        assertNotNull(corsProperties.allowedMethods(), "Allowed methods should not be null");
        assertFalse(corsProperties.allowedMethods().isEmpty(), "Allowed methods should not be empty");

        // Verify allowed headers are configured (should not be wildcard)
        assertNotNull(corsProperties.allowedHeaders(), "Allowed headers should not be null");
        assertFalse(corsProperties.allowedHeaders().isEmpty(), "Allowed headers should not be empty");
        assertFalse(corsProperties.allowedHeaders().contains("*"),
            "Allowed headers should not contain wildcard when credentials are enabled");
    }

    @Test
    void testEitherOriginsOrPatternsAreConfigured() {
        var corsProperties = appProperties.cors();

        boolean hasOrigins = corsProperties.allowedOrigins() != null &&
                            !corsProperties.allowedOrigins().isEmpty();
        boolean hasPatterns = corsProperties.allowedOriginPatterns() != null &&
                             !corsProperties.allowedOriginPatterns().isEmpty();

        assertTrue(hasOrigins || hasPatterns,
            "Either allowed origins or allowed origin patterns should be configured");
    }

    @Test
    void testExposedHeadersCanBeConfigured() {
        var corsProperties = appProperties.cors();

        // Exposed headers are optional, but if configured they should not be empty
        if (corsProperties.exposedHeaders() != null) {
            assertFalse(corsProperties.exposedHeaders().isEmpty(),
                "If exposed headers are configured, they should not be empty");
        }
    }
}
