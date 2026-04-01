package net.trackme.meetingservice.services.integration;

import net.trackme.meetingservice.config.TestSecurityConfig;
import net.trackme.meetingservice.services.integration.sso.SsoApiClientImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ActiveProfiles("test")
@RestClientTest(SsoApiClientImpl.class)
@Import({TestSecurityConfig.class, SsoApiClientImplTest.LocalTestConfig.class})
class SsoApiClientImplTest {

    @TestConfiguration
    static class LocalTestConfig {
        @Bean(name = "serviceRestClient")
        public RestClient serviceRestClient(RestClient.Builder builder) {
            return builder.build();
        }
    }

    @Autowired
    private SsoApiClientImpl client;

    @Autowired
    private MockRestServiceServer server;

    @Test
    void getTrackers_success() {
        String jsonResponse = """
                {
                    "content": [
                        {
                            "id": "123e4567-e89b-12d3-a456-426614174000",
                            "username": "t1",
                            "fullName": "Tracker 1"
                        }
                    ]
                }
                """;

        server.expect(requestTo(org.hamcrest.Matchers.containsString("/api/v1/users/trackers")))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        // Act
        var trackers = client.getTrackers();

        assertNotNull(trackers);
        assertFalse(trackers.isEmpty());
        assertEquals("t1", trackers.get(0).getUsername());

        server.verify();
    }
}