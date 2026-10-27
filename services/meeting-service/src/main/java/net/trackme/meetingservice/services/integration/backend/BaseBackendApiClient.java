package net.trackme.meetingservice.services.integration.backend;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;
import java.util.UUID;

import net.trackme.meetingservice.services.integration.backend.dto.TeamCardDto;
import net.trackme.meetingservice.services.integration.backend.exceptions.TeamCardNotFoundException;
import net.trackme.meetingservice.services.integration.exceptions.IntegrationException;

@Slf4j
public abstract class BaseBackendApiClient {
    protected final RestClient restClient;
    protected static final String SERVICE_NAME = "trackme-backend";

    protected BaseBackendApiClient(RestClient restClient) {
        this.restClient = restClient;
    }

    protected TeamCardDto fetchTeamCard(UUID id) {
        log.debug("Запрос карточки команды id={} из сервиса {}", id, SERVICE_NAME);
        try {
            TeamCardDto result = restClient.get()
                    .uri("/api/v1/team-card?id={id}", id)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (req, res) -> {
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