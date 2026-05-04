package net.trackme.meetingservice.services.integration.backend;

import net.trackme.meetingservice.services.integration.backend.dto.TeamCardDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Service("userBackendApiClient")
public class UserBackendApiClient extends BaseBackendApiClient implements BackendApiClient {
    public UserBackendApiClient(
            @Qualifier("userRestClient") RestClient client,
            @Value("${BACKEND_INNER_URI:http://trackme-backend:8080}") String uri) {
        super(client.mutate().baseUrl(uri).build());
    }

    @Override
    public TeamCardDto getTeamCardById(UUID id) {
        return fetchTeamCard(id);
    }
}