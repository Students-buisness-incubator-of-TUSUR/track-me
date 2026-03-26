package net.trackme.meetingservice.services.integration.backend;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import net.trackme.meetingservice.services.integration.SecurityPropagationInterceptor;
import net.trackme.meetingservice.services.integration.backend.dto.TeamCardDto;
import net.trackme.meetingservice.services.integration.backend.exceptions.TeamCardNotFoundException;
import net.trackme.meetingservice.services.integration.exceptions.IntegrationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class BackendApiClientImpl implements BackendApiClient {
    private final RestClient restClient;
    private static final String SERVICE_NAME = "trackme-backend";

    public BackendApiClientImpl(
            RestClient.Builder restClientBuilder,
            SecurityPropagationInterceptor securityPropagationInterceptor,
            @Value("${BACKEND_INNER_URI:http://trackme-backend:8080}") String backendUri) {

        this.restClient = restClientBuilder
                .requestInterceptor(securityPropagationInterceptor)
                .baseUrl(backendUri)
                .build();
    }

    public TeamCardDto getTeamCardById(UUID id) {
        log.debug("Запрос карточки команды id={} из сервиса {}", id, SERVICE_NAME);
        try {
            TeamCardDto result = restClient.get()
                    .uri("/api/v1/team-card?id={id}", id)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, response) -> {
                        throw new TeamCardNotFoundException(id);
                    })
                    .body(TeamCardDto.class);

            log.debug("Карточка команды id={} успешно получена", id);
            return result;
        } catch (TeamCardNotFoundException e) {
            log.warn("Карточка команды id={} не найдена в сервисе {}", id, SERVICE_NAME);
            throw e;
        } catch (Exception e) {
            log.error("Сетевая ошибка при обращении к сервису {}: {}", SERVICE_NAME, e.getMessage());
            throw new IntegrationException(SERVICE_NAME, e);
        }
    }
}
