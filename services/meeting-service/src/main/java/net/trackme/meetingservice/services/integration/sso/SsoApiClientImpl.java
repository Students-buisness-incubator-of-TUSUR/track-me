package net.trackme.meetingservice.services.integration.sso;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.trackme.meetingservice.services.integration.exceptions.IntegrationException;
import net.trackme.meetingservice.services.integration.sso.dto.UserDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SsoApiClientImpl implements SsoApiClient {
    private final RestClient restClient;
    private static final String SERVICE_NAME = "trackme-sso";

    public SsoApiClientImpl(
            @Qualifier("serviceRestClient") RestClient serviceRestClient,
            @Value("${SSO_INNER_URI:http://trackme-sso:9000}") String ssoUri) {

        this.restClient = serviceRestClient
                .mutate()
                .baseUrl(ssoUri)
                .build();
    }

    @Override
    public List<UserDto> getTrackers() {
        log.debug("Запрос списка трекеров из сервиса {}", SERVICE_NAME);
        try {
            SsoPagedResponse<UserDto> response = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/users/trackers")
                            .queryParam("size", 1000000)
                            .build())
                    .body(Map.of("filters", List.of()))
                    .retrieve()
                    .body(new ParameterizedTypeReference<SsoPagedResponse<UserDto>>() {});


            if (response != null && response.getContent() != null) {
                var trackers = response.getContent();

                log.debug("Список трекеров успешно получен. Количество: {}", trackers.size());
                return response.getContent();
            }

            return List.of();
        } catch (Exception e) {
            log.error("Ошибка при обращении к сервису {}: {}", SERVICE_NAME, e.getMessage());
            throw new IntegrationException(SERVICE_NAME, e);
        }
    }

    @Data
    private static class SsoPagedResponse<T> {
        private List<T> content;
    }
}