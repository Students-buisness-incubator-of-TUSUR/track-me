package net.trackme.meetingservice.services.integration;

import net.trackme.meetingservice.services.integration.backend.BackendApiClient;
import net.trackme.meetingservice.services.integration.backend.BackendApiClientImpl;
import net.trackme.meetingservice.services.integration.backend.dto.TeamCardDto;
import net.trackme.meetingservice.services.integration.backend.exceptions.TeamCardNotFoundException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.client.MockRestServiceServer;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ActiveProfiles("test")
@RestClientTest(BackendApiClientImpl.class)
public class BackendApiClientImplTest {

    @Autowired
    private BackendApiClient client;

    @MockitoBean
    private SecurityPropagationInterceptor securityPropagationInterceptor;

    @Autowired
    private MockRestServiceServer server;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        when(securityPropagationInterceptor.intercept(any(), any(), any()))
                .thenAnswer(invocation -> {
                    var request = invocation.getArgument(0, org.springframework.http.HttpRequest.class);
                    var body = invocation.getArgument(1, byte[].class);
                    var execution = invocation.getArgument(2, org.springframework.http.client.ClientHttpRequestExecution.class);
                    return execution.execute(request, body);
                });
    }

    @Test
    public void getTeamCardById_success() throws Exception {
        UUID id = UUID.randomUUID();
        var expectedDto = new TeamCardDto();
        expectedDto.setId(id);

        server.expect(requestTo(containsString("/api/v1/team-card?id=" + id)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(expectedDto), MediaType.APPLICATION_JSON));

        // Act
        var result = client.getTeamCardById(id);

        // Assert
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
