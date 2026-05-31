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
 * Предоставляет методы для получения команд пользователя
 * и переназначения команд.
 */
@Service
@RequiredArgsConstructor
public class BackendClientImpl implements BackendClient {

    /** Заголовок для передачи токена авторизации. */
    private static final String AUTH_HEADER = "Authorization";

    /** Префикс для Bearer-токена. */
    private static final String BEARER_PREFIX = "Bearer ";

    /** REST-клиент для взаимодействия с бэкендом. */
    private final RestClient restClient;

    /**
     * Получает список команд пользователя из backend-сервиса.
     *
     * @param username имя пользователя
     * @param bearerToken токен авторизации
     * @return список команд пользователя
     */
    @Override
    public List<Map<String, String>> getUserTeams(
            String username, String bearerToken) {
        return restClient.get()
                .uri("/api/v1/admin/team-cards/by-user?username={username}",
                        username)
                .accept(MediaType.APPLICATION_JSON)
                .header(AUTH_HEADER, BEARER_PREFIX + bearerToken)
                .retrieve()
                .body(new ParameterizedTypeReference<
                        List<Map<String, String>>>() { });
    }

    /**
     * Переназначает команды на пользователя Ronin.
     *
     * @param request данные запроса на переназначение
     * @param bearerToken токен авторизации
     */
    @Override
    public void reassignTeamsToRonin(
            Map<String, String> request, String bearerToken) {
        restClient.post()
                .uri("/api/v1/admin/team-cards/reassign")
                .contentType(MediaType.APPLICATION_JSON)
                .header(AUTH_HEADER, BEARER_PREFIX + bearerToken)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }
}
