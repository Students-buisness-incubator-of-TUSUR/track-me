package net.trackme.sso.services.impl;

import lombok.RequiredArgsConstructor;
import net.trackme.sso.services.BackendClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Реализация клиента для взаимодействия с backend-сервисом.
 */
@Service
@RequiredArgsConstructor
public class BackendClientImpl implements BackendClient {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final RestClient restClient;

    @Override
    public List<Map<String, String>> getUserTeams(String username, String bearerToken) {
        return restClient.get()
            .uri("/api/v1/admin/team-cards/by-user?username={username}", username)
            .accept(MediaType.APPLICATION_JSON)
            .header(AUTH_HEADER, BEARER_PREFIX + bearerToken)
            .retrieve()
            .body(new ParameterizedTypeReference<List<Map<String, String>>>() {});
    }

    @Override
    public void reassignTeamsToRonin(Map<String, String> request, String bearerToken) {
        restClient.post()
            .uri("/api/v1/admin/team-cards/reassign")
            .contentType(MediaType.APPLICATION_JSON)
            .header(AUTH_HEADER, BEARER_PREFIX + bearerToken)
            .body(request)
            .retrieve()
            .toBodilessEntity();
    }
}
