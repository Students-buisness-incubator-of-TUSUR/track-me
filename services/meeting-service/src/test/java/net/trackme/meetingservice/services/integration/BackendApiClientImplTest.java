package net.trackme.meetingservice.services.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.trackme.meetingservice.config.TestSecurityConfig;
import net.trackme.meetingservice.services.integration.backend.UserBackendApiClient;
import net.trackme.meetingservice.services.integration.backend.dto.TeamCardDto;
import net.trackme.meetingservice.services.integration.backend.exceptions.TeamCardNotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ActiveProfiles("test")
@RestClientTest(UserBackendApiClient.class)
@Import({TestSecurityConfig.class, BackendApiClientImplTest.LocalTestConfig.class})
public class BackendApiClientImplTest {

    @TestConfiguration
    static class LocalTestConfig {
        @Bean(name = "userRestClient")
        public RestClient userRestClient(RestClient.Builder builder) {
            return builder.build();
        }
    }

    @Autowired
    private UserBackendApiClient client;

    @Autowired
    private MockRestServiceServer server;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void getTeamCardById_success() throws Exception {
        UUID id = UUID.randomUUID();
        var expectedDto = new TeamCardDto();
        expectedDto.setId(id);

        server.expect(requestTo(containsString("/api/v1/team-card?id=" + id)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(expectedDto), MediaType.APPLICATION_JSON));

        var result = client.getTeamCardById(id);

        Assertions.assertEquals(id, result.getId());
        server.verify();
    }

    @Test
    public void getTeamCardById_notFound_throwsException() {
        UUID id = UUID.randomUUID();

        server.expect(requestTo(containsString("/api/v1/team-card")))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        Assertions.assertThrows(TeamCardNotFoundException.class, () -> client.getTeamCardById(id));
        server.verify();
    }
}